package com.kafkaspider.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class BatchKafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers="host.docker.internal:9092";

    @Bean
    KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>>
    batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(batchConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(30000);
        factory.setBatchListener(true);//批量消费
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> batchConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(batchConsumerConfigs());
    }

    @Bean
    public Map<String, Object> batchConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50); //设置每次接收Message的数量
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 120000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 180000);
        return props;
    }
}
