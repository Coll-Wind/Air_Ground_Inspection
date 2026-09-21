package com.agi.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka 消息生产者 - 设备仿真数据上报、任务指令下发
 */
@Slf4j
@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送设备注册消息
     */
    public void sendDeviceRegister(Map<String, Object> message) {
        send("device-register", message);
    }

    /**
     * 发送设备心跳消息
     */
    public void sendHeartbeat(Map<String, Object> message) {
        send("device-heartbeat", message);
    }

    /**
     * 发送设备巡检数据消息
     */
    public void sendDeviceData(Map<String, Object> message) {
        send("device-data", message);
    }

    /**
     * 发送告警消息
     */
    public void sendAlert(Map<String, Object> message) {
        send("device-alert", message);
    }

    /**
     * 下发任务指令到设备
     */
    public void sendTaskDispatch(Map<String, Object> message) {
        send("task-dispatch", message);
    }

    private void send(String topic, Map<String, Object> message) {
        try {
            String key = message.get("deviceCode") != null ? message.get("deviceCode").toString() : null;
            kafkaTemplate.send(topic, key, message);
            log.debug("Kafka 发送成功 topic={}, key={}", topic, key);
        } catch (Exception e) {
            log.error("Kafka 发送失败 topic={}, error={}", topic, e.getMessage(), e);
        }
    }
}
