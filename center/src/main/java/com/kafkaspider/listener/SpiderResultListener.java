package com.kafkaspider.listener;

import com.google.gson.Gson;
import com.kafkaspider.config.KafkaTopic;
import com.kafkaspider.entity.SpiderRecord;
import com.kafkaspider.message.SpiderResultMessage;
import com.kafkaspider.service.sql.SpiderRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class SpiderResultListener {

    @Autowired
    private SpiderRecordService spiderRecordService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private Gson gson;

    @KafkaListener(
            id = "BatchSpiderResultConsumer",
            topics = KafkaTopic.spiderresult,
            containerFactory = "batchFactory",
            properties={
                    "max.poll.interval.ms:30000",
                    "max.poll.records:100",
                    "max.partition.fetch.bytes:10485760"//max.partition.fetch.bytes 1048576 =1MB
            }
    )
    public void batchListenSpiderResult(List<ConsumerRecord<String, String>> list){
        log.info("SpiderResultConsumer receive:" + list.size());
        List<String> messages=list.stream().map(a->a.value()).collect(Collectors.toList());
        log.warn("SpiderResultConsumer RAW:"+gson.toJson(messages));

        for(String message:messages){
            SpiderResultMessage spiderResultMessage = null;
            try {
                spiderResultMessage = gson.fromJson(message, SpiderResultMessage.class);
                log.warn("spiderRecord:" + gson.toJson(spiderResultMessage));
            } catch (Exception e) {
                continue;
            }
            String url = spiderResultMessage.getUrl();
            saveSpiderRecord(spiderResultMessage);

            if (spiderResultMessage.getValid().equals(0)) {
                log.warn("invalid url:" + url + " " + gson.toJson(spiderResultMessage));
            }
        }

    }


    //@KafkaListener(id = "SpiderResultConsumer", topics = KafkaTopic.spiderresult,containerFactory = "")
    public void listenSpiderResult(String message) throws Exception {
        log.info("SpiderResultConsumer receive:" + message);
        SpiderResultMessage spiderResultMessage = null;
        try {
            spiderResultMessage = gson.fromJson(message, SpiderResultMessage.class);
            log.warn("spiderRecord:" + gson.toJson(spiderResultMessage));
        } catch (Exception e) {
            spiderResultMessage = null;
        }

        if (spiderResultMessage == null) {
            return;
        }
        String url = spiderResultMessage.getUrl();
        saveSpiderRecord(spiderResultMessage);

        if (spiderResultMessage.getValid().equals(0)) {
            log.warn("invalid url:" + url + " " + gson.toJson(spiderResultMessage));
            return;
        }
    }

    public void saveSpiderRecord(SpiderResultMessage res) {
        Date date = new Date();
        //插入当前url的爬虫记录
        SpiderRecord spiderRecord = new SpiderRecord();
        spiderRecord.setUrl(res.getUrl());

        spiderRecord.setTag(res.getTag());
        spiderRecord.setTitle(res.getTitle());
        spiderRecord.setContent(res.getContent());
        spiderRecord.setView(res.getView());
        spiderRecord.setTime(res.getTime());

        spiderRecord.setCreateTime(date);
        spiderRecord.setUpdateTime(date);
        spiderRecord.setValid(1);
        if (res.getContent() == null || res.getTitle() == null || res.getContent().length() == 0 || res.getTitle().length() == 0) {
            spiderRecord.setValid(0);//无效文本
        } else if (res.getContent().length() < 150) {
            spiderRecord.setValid(2);//短文本
        }
        boolean op = spiderRecordService.save(spiderRecord);
        log.info("spiderRecordService.save: " + op + " valid:" + spiderRecord.getValid());
    }
}
