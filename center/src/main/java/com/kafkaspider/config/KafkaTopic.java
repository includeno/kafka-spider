package com.kafkaspider.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaTopic {

    public static final Integer count=3;
    public static final Integer replicas=1;

    @Bean
    public NewTopic updateSpark() {
        return TopicBuilder.name(KafkaTopicString.updateSpark)
                .partitions(count)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic sparkPairAnalyze() {
        return TopicBuilder.name(KafkaTopicString.sparkPairAnalyze)
                .partitions(count)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic sparkPairAnalyzeResult() {
        return TopicBuilder.name(KafkaTopicString.sparkPairAnalyzeResult)
                .partitions(count)
                .replicas(replicas)
                .build();
    }
}
