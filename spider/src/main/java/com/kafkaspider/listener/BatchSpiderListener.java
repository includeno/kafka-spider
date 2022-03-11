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
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class BatchSpiderListener {
    static ConcurrentHashMap<String,Long> times=new ConcurrentHashMap<>();

    @Autowired
    Gson gson;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    CommonPageService commonPageService;

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
                    "max.poll.records:10",
                    "auto.commit.interval.ms:100",
                    "session.timeout.ms:60000"
            }
    )
    public void batchSpiderTask(List<String> messages) throws Exception {
        ConcurrentHashMap<Future<UrlRecord>,String> back=new ConcurrentHashMap<>();
        log.info("batchSpiderTask receive "+messages.size());
        //去重
        HashSet<String> set=new HashSet<>();
        set.addAll(messages);
        List<String> list = set.stream().distinct().collect(Collectors.toList());//url过滤重复url
        log.info("batchSpiderTask after filter "+messages.size());
        log.info("batchSpiderTask after filter list:"+ Arrays.toString(list.toArray()));

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 10, 6, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        CountDownLatch downLatch=new CountDownLatch(messages.size());
        List<Callable<UrlRecord>> tasks=new ArrayList<>();
        List<Future<UrlRecord>> futures=new ArrayList<>();
        for(String url:list){
            if(url.replace(" ","").equals("")){
                log.warn("skip blank");
                continue;
            }
            log.info("add task:"+url);
            Callable<UrlRecord> task = getTask(downLatch, url);
            tasks.add(task);
            Future<UrlRecord> future=executor.submit(task);
            futures.add(future);
            back.putIfAbsent(future,url);
        }
        log.info("executor size: futures"+futures.size()+" tasks:"+tasks.size()+" back:"+back.size());

        boolean downLatcherror=false;
        try {
            long start=System.currentTimeMillis();
            //等待全部处理完成
            downLatch.await(120,TimeUnit.SECONDS);
            log.warn("downLatch completed!");

            //获取全部处理结果
            List<UrlRecord> records=new ArrayList<>();
            for(Future<UrlRecord> future:futures){
                records.add(future.get());
            }
            for(UrlRecord record:records){
                log.info("record url after downLatch:"+record.getUrl());
                SpiderResponse response = normal(record);

                if(response.getCode().equals(SpiderCode.SPIDER_UNREACHABLE.getCode())){
                    //遇到错误，重新发送任务
                    ListenableFuture task = kafkaTemplate.send(KafkaTopicString.spidertask_slow, record.getUrl());
                    try {
                        task.get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                else{
                    resolveNormal(record);
                }

                Long exp=(System.currentTimeMillis()-start);
                times.put("sum", times.getOrDefault("sum",0L)+exp);
                times.put("count",times.getOrDefault("count",0L)+1L);
                times.put("avg",times.get("sum")/times.get("count"));
                times.put("max",Math.max(times.getOrDefault("max",0L),exp));
                log.info("url:"+record.getUrl()+" STAT current:"+exp+" avg:"+times.get("avg")+" count:"+times.get("count")+" max:"+times.get("max"));
            }
        } catch (InterruptedException e) {
            downLatcherror=true;
            log.error("downLatch error!");
            for(Future<UrlRecord> future:futures) {
                if (!future.isDone()) {
                    String url=back.get(future);

                    UrlRecord record=new UrlRecord();
                    record.setUrl(url);
                    log.info("record url when downLatcherror:"+record.getUrl());
                    resolveError(record);//非正常结束
                }
                else{
                    UrlRecord record = future.get();
                    log.info("record url when downLatcherror future.get():"+record.getUrl());
                    resolveNormal(record);//正常结束
                }
            }
        }
        finally {
            executor.shutdownNow();
        }
    }

    //正常处理
    public SpiderResponse normal(UrlRecord record){
        SpiderResponse response=new SpiderResponse();
        String simhash="";
        if(record!=null&&(record.getTitle()==null||record.getContent()==null||record.getTime()==null)){
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

        return response;
    }

    public void resolveNormal(UrlRecord record){
        SpiderResponse response=normal(record);
        SpiderResultMessage spiderResultMessage = SpiderResultMessage.copyUrlRecord(record);
        spiderResultMessage.setMessage(response.getMessage());
        spiderResultMessage.setCode(response.getCode());
        spiderResultMessage.setSimhash("");
        ListenableFuture task= kafkaTemplate.send(KafkaTopicString.spiderresult, gson.toJson(spiderResultMessage));
        try {
            task.get();
            log.info("SpiderResultMessage success " + record.getUrl());
        } catch (InterruptedException e1) {
            log.error("InterruptedException SpiderResultMessage send_error " + record.getUrl() + " " + e1.getMessage());
        } catch (ExecutionException e2) {
            log.error("InterruptedException SpiderResultMessage send_error " + record.getUrl() + " " + e2.getMessage());
        }
    }

    public void resolveError(UrlRecord record){
        kafkaTemplate.send(KafkaTopicString.spidertask_slow, record.getUrl()).addCallback(new SuccessCallback() {
            @Override
            public void onSuccess(Object o) {
                log.info("SPIDER_UNREACHABLE send_success " + record.getUrl());
            }
        }, new FailureCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                log.error("SPIDER_UNREACHABLE send_error " + record.getUrl() + " " + throwable.getMessage());
            }
        });
        kafkaTemplate.flush();
    }

    public Callable<UrlRecord> getTask(CountDownLatch countDownLatch,String message) {
        Callable<UrlRecord> callable = () -> {
            log.info("spider receive:" + message);
            String url = message;
            UrlRecord record = new UrlRecord();
            record.setUrl(url);
            boolean error=false;
            try {
                log.info("before crawl "+url);
                record=commonPageService.crawl(record);
                log.info("after crawl "+url);
            }
            catch (Exception e){
                log.error("commonPageService.crawl error"+url);
                error=true;
            }
            finally {
                countDownLatch.countDown();
                log.info("return crawl:"+url);
                return record;
            }
        };
        return callable;
    }
}
