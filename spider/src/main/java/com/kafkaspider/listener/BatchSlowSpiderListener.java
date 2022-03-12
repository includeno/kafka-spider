package com.kafkaspider.listener;

import com.google.gson.Gson;
import com.kafkaspider.config.KafkaTopicString;
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class BatchSlowSpiderListener implements BatchListener{
    static ConcurrentHashMap<String,Long> times=new ConcurrentHashMap<>();
    static ConcurrentHashMap<String,Integer> back=new ConcurrentHashMap<>();

    @Autowired
    Gson gson;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    SpiderWorker spiderWorker;

    @KafkaListener(
            id = "SlowSpidertaskConsumer",
            topics = KafkaTopicString.spidertask_slow,
            containerFactory = "batchFactory",
            properties={
                    "fetch.max.wait.ms:500",
                    "max.poll.interval.ms:180000",
                    "max.poll.records:4",
                    "auto.commit.interval.ms:100",
                    "session.timeout.ms:120000"
            }
    )
    public void batchSlowSpiderTask(List<String> messages) throws ExecutionException, InterruptedException {
        log.info("batchSlowSpiderTask receive "+messages.size());
        ConcurrentHashMap<String,Long> timeRecord=new ConcurrentHashMap<>();
        timeRecord.put("start",System.currentTimeMillis());//记录起始时间

        ConcurrentHashMap<Future<UrlRecord>,String> back=new ConcurrentHashMap<>();
        //去重
        HashSet<String> set=new HashSet<>();
        set.addAll(messages);
        List<String> list = set.stream().distinct().collect(Collectors.toList());//url过滤重复url
        log.info("batchSlowSpiderTask after filter" + gson.toJson(list));

        ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 6, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        CountDownLatch downLatch=new CountDownLatch(list.size());
        List<Callable<UrlRecord>> tasks=new ArrayList<>();
        List<Future<UrlRecord>> futures=new ArrayList<>();
        for(String url:list){
            if(url.replace(" ","").equals("")){
                log.warn("skip blank");
                continue;
            }
            Callable<UrlRecord> task = spiderWorker.getTask(downLatch, url);
            tasks.add(task);
            Future<UrlRecord> future=executor.submit(task);
            futures.add(future);
            back.putIfAbsent(future,url);
            log.info("batchSlowSpiderTask add task:"+url);
        }
        log.info("batchSlowSpiderTask executor size: futures"+futures.size()+" tasks:"+tasks.size()+" back:"+back.size());

        try {
            //等待全部处理完成
            downLatch.await(160,TimeUnit.SECONDS);
            log.warn("downLatch completed!");
        } catch (InterruptedException e) {
            log.error("downLatch error!");
        }
        finally {
            if(executor!=null){
                executor.shutdownNow();
            }
            resolve(back,futures);
            Long exp=(System.currentTimeMillis()-timeRecord.get("start"));
            times.put("sum", times.getOrDefault("sum",0L)+exp);
            times.put("count",times.getOrDefault("count",0L)+1L);
            times.put("avg",times.get("sum")/times.get("count"));
            times.put("max",Math.max(times.getOrDefault("max",0L),exp));
            log.info("STAT current:"+exp+" avg:"+times.get("avg")+" count:"+times.get("count")+" max:"+times.get("max"));
        }
    }

    //变成响应类型
    public SpiderResponse generateResponse(UrlRecord record){
        SpiderResponse response=new SpiderResponse();
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

    public void resolve(ConcurrentHashMap<Future<UrlRecord>,String> back,List<Future<UrlRecord>> futures) throws ExecutionException, InterruptedException {
        for(Future<UrlRecord> future:futures) {
            if (!future.isDone()) {
                String url=back.get(future);

                UrlRecord record=new UrlRecord();
                record.setUrl(url);
                log.info("record url when downLatcherror:"+record.getUrl());
                resolveNormal(record);//在慢速模式下无论什么情况都需要传递结果
            }
            else{
                UrlRecord record = future.get();
                log.info("record url when downLatcherror future.get():"+record.getUrl());
                resolveNormal(record);//在慢速模式下无论什么情况都需要传递结果
            }
        }
    }
    @Override
    public void resolveNormal(UrlRecord record){
        SpiderResponse response= generateResponse(record);
        SpiderResultMessage spiderResultMessage = SpiderResultMessage.copyUrlRecord(record);
        spiderResultMessage.setMessage(response.getMessage());
        spiderResultMessage.setCode(response.getCode());
        spiderResultMessage.setSimhash("");
        ListenableFuture task= kafkaTemplate.send(KafkaTopicString.spiderresult, gson.toJson(spiderResultMessage));
        try {
            task.get();
            log.info("BatchSlowSpiderListener SpiderResultMessage success " + record.getUrl());
        } catch (InterruptedException e1) {
            log.error("BatchSlowSpiderListener InterruptedException SpiderResultMessage send_error " + record.getUrl() + " " + e1.getMessage());
        } catch (ExecutionException e2) {
            log.error("BatchSlowSpiderListener InterruptedException SpiderResultMessage send_error " + record.getUrl() + " " + e2.getMessage());
        }
    }

    @Override
    public void resolveError(UrlRecord record) {
        resolveNormal(record);
    }
}
