package com.kafkaspider.listener;

import com.google.gson.Gson;
import com.kafkaspider.config.KafkaTopic;
import com.kafkaspider.config.KafkaTopicString;
import com.kafkaspider.config.SpiderLimit;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.enums.SpiderCode;
import com.kafkaspider.message.SpiderResultMessage;
import com.kafkaspider.response.SpiderResponse;
import com.kafkaspider.service.CommonPageService;
import com.kafkaspider.worker.SpiderWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Configuration
public class BatchSpiderListener {
    static ConcurrentHashMap<String,Long> times=new ConcurrentHashMap<>();
    static ConcurrentHashMap<String,Integer> back=new ConcurrentHashMap<>();

    @Autowired
    Gson gson;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    CommonPageService commonPageService;

    //18446
    //https://strimzi.io/blog/2021/01/07/consumer-tuning/
    //max.poll.interval.ms 表示 consumer 每两次 poll 消息的时间间隔。简单地说，其实就是 consumer 每次消费消息的时长。如果消息处理的逻辑很重，那么市场就要相应延长。否则如果时间到了 consumer 还么消费完，broker 会默认认为 consumer 死了，发起 rebalance。
    //
    //max.poll.records 表示每次消费的时候，获取多少条消息。获取的消息条数越多，需要处理的时间越长。所以每次拉取的消息数不能太多，需要保证在 max.poll.interval.ms 设置的时间内能消费完，否则会发生 rebalance。

    @KafkaListener(
            id = "SpidertaskConsumer",
            topics = KafkaTopicString.spidertask,
            containerFactory = "batchFactory",
            properties={
                    "fetch.max.wait.ms:500",
                    "max.poll.interval.ms:180000",
                    "max.poll.records:16",
                    "auto.commit.interval.ms:100",
                    "session.timeout.ms:60000"
            }
    )
    public void batchSpiderTask(List<String> messages){
        log.info("batchSpiderTask receive "+messages.size());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 10, 6, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        CountDownLatch downLatch=new CountDownLatch(messages.size());
        try {
            for(String url:messages){
                if(url.equals("")){
                    continue;
                }
                executor.submit(getTask(downLatch,url));
            }
            downLatch.await(120,TimeUnit.SECONDS);
        }
        catch (Exception e){
            log.error("executor error back:"+back.size());
            //备份
        }
        finally {
            executor.shutdownNow();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("BatchSpiderListener back.size():"+back.size());
            for(String url: back.keySet()){
                log.error("BatchSpiderListener未处理完成:"+url);
                kafkaTemplate.send(KafkaTopicString.spidertask_slow, url).addCallback(new SuccessCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        back.put(url,null);
                        log.info("SPIDER_SLOW send_success " + url+" ");
                    }
                }, new FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("SPIDER_SLOW send_error " + url + " " + throwable.getMessage());
                    }
                });
                kafkaTemplate.flush();
            }
            back=new ConcurrentHashMap<>();
        }
    }


    public Runnable getTask(CountDownLatch countDownLatch,String message){
        Runnable runnable = () -> {
            log.info("spider receive:" + message);
            String url = message;
            back.put(url,1);

            long start=System.currentTimeMillis();
            SpiderResponse response=new SpiderResponse();
            UrlRecord record = new UrlRecord();
            record.setUrl(url);
            boolean error=false;
            try {
                record=commonPageService.crawl(record);
            }
            catch (Exception e){
                log.error("commonPageService.crawl error");
                error=true;
                //遇到错误，重新发送任务
                kafkaTemplate.send(KafkaTopicString.spidertask_slow, url).addCallback(new SuccessCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        log.info("spider_slow send success "+url);
                    }
                }, new FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("spider_slow send error "+url+" "+throwable.getMessage());
                    }
                });
            }
            finally {
                String simhash="";
                if(record!=null&&(record.getTitle()==null||record.getContent()==null)){
                    response.setCode(SpiderCode.SPIDER_UNREACHABLE.getCode());
                    response.setRecord(record);
                }
                else if(record!=null&&record.getTitle().length()>0&&record.getContent().length()>0){
                    response.setCode(SpiderCode.SUCCESS.getCode());
                    response.setRecord(record);
                }
                else {
                    response.setCode(SpiderCode.SPIDER_UNREACHABLE.getCode());
                    response.setRecord(record);
                }

                Long exp=(System.currentTimeMillis()-start);
                times.put("sum", times.getOrDefault("sum",0L)+exp);
                times.put("count",times.getOrDefault("count",0L)+1L);
                times.put("avg",times.get("sum")/times.get("count"));
                times.put("max",Math.max(times.getOrDefault("max",0L),exp));
                log.info("url:"+url+" STAT current:"+exp+" avg:"+times.get("avg")+" count:"+times.get("count")+" max:"+times.get("max"));

                if(error==true){
                    return;
                }
                SpiderResultMessage spiderResultMessage = SpiderResultMessage.copyUrlRecord(record);
                spiderResultMessage.setMessage(response.getMessage());
                spiderResultMessage.setCode(response.getCode());
                spiderResultMessage.setSimhash(simhash);
                //步骤6 任务添加至sparktask队列
                kafkaTemplate.send(KafkaTopicString.spiderresult, gson.toJson(spiderResultMessage)).addCallback(new SuccessCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        log.info("SpiderResultMessage send_success " + url);
                        back.put(url,null);
                        countDownLatch.countDown();
                    }
                }, new FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("SpiderResultMessage send_error " + url + " " + throwable.getMessage());
                    }
                });
                kafkaTemplate.flush();
            }
        };
        return runnable;
    }
}
