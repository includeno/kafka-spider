package com.kafkaspider.config;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.kafkaspider.SpiderMain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpiderLimit {
    public static ConcurrentHashSet<String> spiders=new ConcurrentHashSet<>();;
    public static final int countOfSpiderDefault=4;
    public static Integer countOfSpider=countOfSpiderDefault;//默认允许4个爬虫同时运行 --spider.count=6

    //限制同时开启的爬虫数量
    @Value("${spider.count}")
    Integer count;

    @Bean
    public void setSpiderCount(){
        if(count<=0){
            countOfSpider=countOfSpiderDefault;
        }
        else{
            countOfSpider=count;
        }
    }
}
