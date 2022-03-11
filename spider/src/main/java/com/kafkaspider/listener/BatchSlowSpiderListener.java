package com.kafkaspider.listener;

import com.google.gson.Gson;
import com.kafkaspider.config.KafkaTopicString;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.enums.SpiderCode;
import com.kafkaspider.message.SpiderResultMessage;
import com.kafkaspider.response.SpiderResponse;
import com.kafkaspider.service.CommonPageService;
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
public class BatchSlowSpiderListener {
    static ConcurrentHashMap<String,Long> times=new ConcurrentHashMap<>();
    static ConcurrentHashMap<String,Integer> back=new ConcurrentHashMap<>();

    @Autowired
    Gson gson;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    CommonPageService commonPageService;

    @KafkaListener(
            id = "SlowSpidertaskConsumer",
            topics = KafkaTopicString.spidertask_slow,
            containerFactory = "batchFactory",
            properties={
                    "fetch.max.wait.ms:500",
                    "max.poll.interval.ms:300000",
                    "max.poll.records:4",
                    "auto.commit.interval.ms:100",
                    "session.timeout.ms:120000"
            }
    )
    public void batchSlowSpiderTask(List<String> messages){
        log.info("batchSlowSpiderTask receive "+messages.size());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 6, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        CountDownLatch downLatch=new CountDownLatch(messages.size());
        try {
            for(String url:messages){
                if(url.equals("")){
                    continue;
                }
                executor.submit(getTask(downLatch,url));
            }
            downLatch.await(240,TimeUnit.SECONDS);
        }
        catch (Exception e){
            log.error("executor error back:"+back.size());
            //备份
        }
        finally {
            kafkaTemplate.flush();
            executor.shutdownNow();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("BatchSlowSpiderListener back.size():"+back.size());
            for(String url: back.keySet()){
                log.error("BatchSlowSpiderListener未处理完成:"+url);
                SpiderResultMessage spiderResultMessage = new SpiderResultMessage();
                spiderResultMessage.setMessage("无法爬取");
                spiderResultMessage.setCode(SpiderCode.SPIDER_UNREACHABLE.getCode());
                spiderResultMessage.setSimhash("");
                //步骤6 任务添加至sparktask队列
                kafkaTemplate.send(KafkaTopicString.spiderresult, gson.toJson(spiderResultMessage)).addCallback(new SuccessCallback() {
                    @Override
                    public void onSuccess(Object o) {
                        log.info("SPIDER_UNREACHABLE send_success " + url);
                        back.put(url,null);
                    }
                }, new FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.error("SPIDER_UNREACHABLE send_error " + url + " " + throwable.getMessage());
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
            try {
                record=commonPageService.crawl(record);
            }
            catch (Exception e){
                log.error("commonPageService.crawl error");
                //遇到错误，重新发送任务
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
                Long exp=(System.currentTimeMillis()-start);
                times.put("sum", times.getOrDefault("sum",0L)+exp);
                times.put("count",times.getOrDefault("count",0L)+1L);
                times.put("avg",times.get("sum")/times.get("count"));
                times.put("max",Math.max(times.getOrDefault("max",0L),exp));
                log.info("url:"+url+" STAT current:"+exp+" avg:"+times.get("avg")+" count:"+times.get("count")+" max:"+times.get("max"));
            }


        };
        return runnable;
    }
}
