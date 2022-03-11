package com.kafkaspider.service;

import com.google.gson.Gson;
import com.kafkaspider.config.KafkaTopic;
import com.kafkaspider.config.KafkaTopicString;
import com.kafkaspider.request.SubmitRequest;
import com.kafkaspider.util.UrlFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubmitService {

    @Autowired
    private KafkaTemplate kafkaTemplate;
    
    @Autowired
    private Gson gson;

    public void submitFunction(SubmitRequest request){
        HashSet<String> set=new HashSet<>();
        set.addAll(request.getList());
        List<String> list = set.stream().distinct().collect(Collectors.toList());//url过滤重复url
        log.info("/submit begin filter"+gson.toJson(list));
        List<String> ans = null;//筛选出符合条件的URl
        try {
            ans = UrlFilter.filter(list);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        log.info("/submit end filter"+gson.toJson(ans));

        for(String url:ans){
            //发送爬虫请求
            kafkaTemplate.send(KafkaTopicString.spidertask, url).addCallback(new SuccessCallback() {
                @Override
                public void onSuccess(Object o) {
                    log.info("spidertask send success "+url);
                }
            }, new FailureCallback() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.error("spidertask send error "+url+" "+throwable.getMessage());
                }
            });
        }

    }
}
