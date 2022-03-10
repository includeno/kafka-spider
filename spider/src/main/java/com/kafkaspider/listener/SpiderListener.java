package com.kafkaspider.listener;

import com.google.gson.Gson;

import com.kafkaspider.config.KafkaTopic;
import com.kafkaspider.config.SpiderLimit;
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

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class SpiderListener {
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
            topics = KafkaTopic.spidertask,
            properties={
                    "fetch.max.wait.ms:700",
                    "max.poll.interval.ms:300000",
                    "max.poll.records:3",
                    "auto.commit.interval.ms:100",
                    "session.timeout.ms:30000"
            }
    )
    public void spidertask(String message) throws Exception {
        log.info("spider receive:" + message);
        String url = message;

        long start=System.currentTimeMillis();
        SpiderResponse response=new SpiderResponse();
        UrlRecord record = new UrlRecord();
        record.setUrl(url);
        if (SpiderLimit.spiders.size()<SpiderLimit.countOfSpider&&!SpiderLimit.spiders.contains(url)) {
            SpiderLimit.spiders.add(url);
            log.info("SpiderLimit.spiders:"+SpiderLimit.spiders.size());
            try {
                record=commonPageService.crawl(record);
            }
            catch (Exception e){
                log.error("commonPageService.crawl error");
            }
            finally {
                SpiderLimit.spiders.remove(url);
            }
            String simhash="";
            if(record!=null&&(record.getTitle()==null||record.getContent()==null)){
                response.setCode(SpiderCode.SPIDER_UNREACHABLE.getCode());
                response.setRecord(record);
            }
            else if(record!=null&&record.getTitle().length()>0&&record.getContent().length()>0){
                response.setCode(SpiderCode.SUCCESS.getCode());
                response.setRecord(record);
                log.info("crawl result:"+gson.toJson(response));

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
            kafkaTemplate.send(KafkaTopic.spiderresult, gson.toJson(spiderResultMessage)).addCallback(new SuccessCallback() {
                @Override
                public void onSuccess(Object o) {
                    log.info("SpiderResultMessage send_success " + url);
                }
            }, new FailureCallback() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.error("SpiderResultMessage send_error " + url + " " + throwable.getMessage());
                }
            });
            kafkaTemplate.flush();
        }
        else{
            response.setCode(SpiderCode.SPIDER_COUNT_LIMIT.getCode());//因为爬虫服务数量已满
            log.error("SPIDER_COUNT_LIMIT error " + url + " code" + SpiderCode.SPIDER_COUNT_LIMIT.getCode());
            throw new Exception(SpiderCode.SPIDER_COUNT_LIMIT.name());
        }

        Long exp=(System.currentTimeMillis()-start);
        times.put("sum", times.getOrDefault("sum",0L)+exp);
        times.put("count",times.getOrDefault("count",0L)+1L);
        log.info("STAT current:"+exp+" avg:"+times.get("sum")/times.get("count")+" total:"+times.get("sum"));
    }

}
