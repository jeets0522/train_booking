package com.trainbooking.notification.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import org.apache.kafka.clients.admin.NewTopic;

import java.util.HashMap;
import java.util.Map;

/**
 * The notification service is also a producer: it republishes resolved {@link
 * com.trainbooking.notification.kafka.NotificationCommand}s onto the channel/priority delivery
 * topics. Mirrors the accounts serializer setup (JSON, no type-info headers).
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.email-transactional}")
    private String emailTransactionalTopic;

    @Value("${kafka.topics.email-bulk}")
    private String emailBulkTopic;

    @Value("${kafka.topics.sms-transactional}")
    private String smsTransactionalTopic;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Delivery topics — auto-created on startup via KafkaAdmin.

    @Bean
    public NewTopic emailTransactionalTopic() {
        return TopicBuilder.name(emailTransactionalTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic emailBulkTopic() {
        return TopicBuilder.name(emailBulkTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic smsTransactionalTopic() {
        return TopicBuilder.name(smsTransactionalTopic).partitions(3).replicas(1).build();
    }
}
