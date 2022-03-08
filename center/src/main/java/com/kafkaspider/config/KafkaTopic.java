package com.kafkaspider.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaTopic {
    //Spark监听

    public static final String updateSpark ="updateSpark";//跳过爬虫部分直接获取最新爬取结果进行Spark数据分析步骤

    public static final String sparkPairAnalyze="sparkPairAnalyze";//Spark 分析原创文章和非原创文章配对任务
    public static final String sparkPairAnalyzeResult="sparkPairAnalyzeResult";//Spark 分析原创文章和非原创文章配对 处理结果

    public static final Integer count=3;
    public static final Integer replicas=1;

    @Bean
    public NewTopic updateSpark() {
        return TopicBuilder.name(KafkaTopic.updateSpark)
                .partitions(count)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic sparkPairAnalyze() {
        return TopicBuilder.name(KafkaTopic.sparkPairAnalyze)
                .partitions(count)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic sparkPairAnalyzeResult() {
        return TopicBuilder.name(KafkaTopic.sparkPairAnalyzeResult)
                .partitions(count)
                .replicas(replicas)
                .build();
    }

    //spider
    public static final String spidertask ="spidertask";//spidertask
    public static final String spiderresult="spiderresult";//spiderresult
}
