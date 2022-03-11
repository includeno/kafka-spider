package com.kafkaspider.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaTopic {

    @Value("${spider.partition.count}")
    Integer count;

    @Bean
    public NewTopic spidertask() {
        return TopicBuilder.name(KafkaTopicString.spidertask)
                .partitions(count)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic spiderresult() {
        return TopicBuilder.name(KafkaTopicString.spiderresult)
                .partitions(count)
                .replicas(1)
                .build();
    }

}
