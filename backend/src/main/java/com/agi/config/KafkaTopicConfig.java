package com.agi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic 配置 - 自动创建所需 Topic
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.device-register}")
    private String deviceRegisterTopic;

    @Value("${kafka.topics.device-heartbeat}")
    private String deviceHeartbeatTopic;

    @Value("${kafka.topics.device-data}")
    private String deviceDataTopic;

    @Value("${kafka.topics.device-alert}")
    private String deviceAlertTopic;

    @Value("${kafka.topics.task-dispatch}")
    private String taskDispatchTopic;

    @Bean
    public NewTopic deviceRegisterTopic() {
        return TopicBuilder.name(deviceRegisterTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic deviceHeartbeatTopic() {
        return TopicBuilder.name(deviceHeartbeatTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic deviceDataTopic() {
        return TopicBuilder.name(deviceDataTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic deviceAlertTopic() {
        return TopicBuilder.name(deviceAlertTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic taskDispatchTopic() {
        return TopicBuilder.name(taskDispatchTopic).partitions(1).replicas(1).build();
    }
}
