package com.kafkaspider.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;

import com.kafkaspider.config.KafkaTopic;
import com.kafkaspider.config.SpiderLimit;
import com.kafkaspider.entity.SpiderRecord;
import com.kafkaspider.entity.UrlRecord;
import com.kafkaspider.enums.SpiderCode;
import com.kafkaspider.message.SpiderResultMessage;
import com.kafkaspider.response.SpiderResponse;
import com.kafkaspider.service.CommonPageService;
import com.kafkaspider.service.sql.SpiderRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.Date;

@Slf4j
@Configuration
public class SpiderListener {
    @Autowired
    Gson gson;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    CommonPageService commonPageService;

    @Autowired
    SpiderRecordService spiderRecordService;

    @KafkaListener(id = "SpidertaskConsumer", topics = KafkaTopic.spidertask)
    public void spidertask(String message) throws Exception {
        log.info("spider receive:" + message);
        String url = message;

        long start=System.currentTimeMillis();
        log.info("crawl begin:"+url+" "+start);
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
                    log.info("SpiderResultMessage send success " + url);
                }
            }, new FailureCallback() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.error("SpiderResultMessage send error " + url + " " + throwable.getMessage());
                }
            });
            kafkaTemplate.flush();
        }
        else{
            response.setCode(SpiderCode.SPIDER_COUNT_LIMIT.getCode());//因为爬虫服务数量已满
            //throw new Exception(SpiderCode.SPIDER_COUNT_LIMIT.name());
        }
        log.info("crawl end:"+url+" "+(System.currentTimeMillis()-start));
    }



    public void saveSpiderRecordOld(UrlRecord res) {
        Date date = new Date();
        //保存当前url的爬虫记录
        QueryWrapper<SpiderRecord> spiderQueryWrapper = new QueryWrapper();
        spiderQueryWrapper.eq("url", res.getUrl());
        SpiderRecord spiderRecord = spiderRecordService.getOne(spiderQueryWrapper);
        if (spiderRecord == null) {
            //无记录
            spiderRecord = new SpiderRecord();
            spiderRecord.setUrl(res.getUrl());

            spiderRecord.setTag(res.getTag());
            spiderRecord.setTitle(res.getTitle());
            spiderRecord.setContent(res.getContent());
            spiderRecord.setTime(res.getTime());

            spiderRecord.setCreateTime(date);
            spiderRecord.setUpdateTime(date);
            spiderRecord.setValid(1);
            if(res.getContent().length()==0||res.getTitle().length()==0){
                spiderRecord.setValid(0);
            }
            boolean op = spiderRecordService.save(spiderRecord);
            log.info("spiderRecordService.save: " + op);
        } else {
            spiderRecord.setTag(res.getTag());
            spiderRecord.setContent(res.getContent());
            spiderRecord.setTime(res.getTime());

            spiderRecord.setUpdateTime(date);
            spiderRecord.setValid(1);
            if(res.getContent().length()==0||res.getTitle().length()==0){
                spiderRecord.setValid(0);
            }
            boolean op = spiderRecordService.updateById(spiderRecord);
            log.info("spiderRecordService.updateById: " + op);
        }
    }
}
