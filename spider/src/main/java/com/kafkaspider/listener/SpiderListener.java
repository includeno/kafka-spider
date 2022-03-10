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

    @KafkaListener(
            id = "SpidertaskConsumer",
            topics = KafkaTopic.spidertask,
            properties={
                    "max.poll.interval.ms:60000",
                    "max.poll.records:1"
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
            record=commonPageService.crawl(record);
            SpiderLimit.spiders.remove(url);

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
