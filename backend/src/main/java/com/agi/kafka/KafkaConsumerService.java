package com.agi.kafka;

import com.agi.es.EsService;
import com.agi.model.Alert;
import com.agi.model.Device;
import com.agi.model.InspectionRecord;
import com.agi.model.InspectionTask;
import com.agi.repository.AlertRepository;
import com.agi.repository.DeviceRepository;
import com.agi.repository.InspectionRecordRepository;
import com.agi.repository.TaskRepository;
import com.agi.websocket.AlertWebSocketHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka 消息消费者 - 消费设备上报数据并落库
 * 使用 StringDeserializer 接收 JSON 字符串,手动解析避免 type header 问题
 */
@Slf4j
@Component
public class KafkaConsumerService {

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private InspectionRecordRepository recordRepository;
    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EsService esService;
    @Autowired
    private AlertWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 消费设备注册消息 -> 写入 MongoDB 设备台账
     */
    @KafkaListener(topics = "${kafka.topics.device-register}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRegister(String message) {
        Map<String, Object> msg = parse(message);
        if (msg == null) return;
        try {
            String deviceCode = (String) msg.get("deviceCode");
            Device device = new Device();
            device.setId(deviceCode);
            device.setDeviceCode(deviceCode);
            device.setDeviceType((String) msg.get("deviceType"));
            device.setName((String) msg.get("name"));
            device.setModel((String) msg.get("model"));
            device.setStatus("ONLINE");
            device.setBattery(toInt(msg.get("battery")));
            device.setLatitude(toDouble(msg.get("latitude")));
            device.setLongitude(toDouble(msg.get("longitude")));
            device.setRegisterTime(LocalDateTime.now());
            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device);
            log.info("设备注册成功: {} ({})", deviceCode, device.getDeviceType());
        } catch (Exception e) {
            log.error("消费设备注册消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 消费心跳消息 -> 更新设备状态与最后心跳时间
     * 设备状态以设备自报为准(FAULT 表示故障驻留检修中,不可强制置回 ONLINE)
     */
    @KafkaListener(topics = "${kafka.topics.device-heartbeat}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeHeartbeat(String message) {
        Map<String, Object> msg = parse(message);
        if (msg == null) return;
        try {
            String deviceCode = (String) msg.get("deviceCode");
            Object statusObj = msg.get("status");
            String status = statusObj == null ? "ONLINE" : statusObj.toString();
            deviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> {
                device.setStatus(status);
                device.setBattery(toInt(msg.get("battery")));
                device.setLatitude(toDouble(msg.get("latitude")));
                device.setLongitude(toDouble(msg.get("longitude")));
                device.setLastHeartbeat(LocalDateTime.now());
                deviceRepository.save(device);
            });
            log.debug("心跳更新: {} 状态 {}", deviceCode, status);
        } catch (Exception e) {
            log.error("消费心跳消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 消费巡检数据消息 -> 写入 MongoDB 巡检记录
     */
    @KafkaListener(topics = "${kafka.topics.device-data}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeDeviceData(String message) {
        Map<String, Object> msg = parse(message);
        if (msg == null) return;
        try {
            InspectionRecord record = new InspectionRecord();
            record.setId(UUID.randomUUID().toString());
            record.setRecordCode("REC-" + System.currentTimeMillis());
            record.setDeviceCode((String) msg.get("deviceCode"));
            record.setDeviceType((String) msg.get("deviceType"));
            record.setLatitude(toDouble(msg.get("latitude")));
            record.setLongitude(toDouble(msg.get("longitude")));
            record.setPayload(String.valueOf(msg.get("payload")));
            record.setImagePath((String) msg.get("imagePath"));
            record.setReportTime(LocalDateTime.now());
            recordRepository.save(record);
            log.info("巡检记录入库: {} - {}", record.getDeviceCode(), record.getRecordCode());
        } catch (Exception e) {
            log.error("消费巡检数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 消费告警消息 -> 写入 MongoDB + 索引到 Elasticsearch + WebSocket 推送前端
     */
    @KafkaListener(topics = "${kafka.topics.device-alert}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAlert(String message) {
        Map<String, Object> msg = parse(message);
        if (msg == null) return;
        try {
            Alert alert = new Alert();
            alert.setId(UUID.randomUUID().toString());
            alert.setAlertCode("ALT-" + System.currentTimeMillis());
            alert.setDeviceCode((String) msg.get("deviceCode"));
            alert.setDeviceType((String) msg.get("deviceType"));
            alert.setAlertType((String) msg.get("alertType"));
            alert.setLevel((String) msg.get("level"));
            alert.setDescription((String) msg.get("description"));
            alert.setLatitude(toDouble(msg.get("latitude")));
            alert.setLongitude(toDouble(msg.get("longitude")));
            alert.setStatus("PENDING");
            alert.setAlertTime(LocalDateTime.now());
            alertRepository.save(alert);

            // 设备故障告警:立即同步设备台账状态为 FAULT(与告警自洽)
            if ("DEVICE_FAULT".equals(alert.getAlertType())) {
                deviceRepository.findByDeviceCode(alert.getDeviceCode()).ifPresent(d -> {
                    d.setStatus("FAULT");
                    d.setLastHeartbeat(LocalDateTime.now());
                    deviceRepository.save(d);
                });
            }

            // 写入 Elasticsearch(失败重试一次,保证检索数据尽量不丢)
            try {
                esService.indexAlert(alert);
            } catch (Exception esEx) {
                log.warn("告警写入 ES 失败,准备重试: {}", esEx.getMessage());
                try {
                    Thread.sleep(500);
                    esService.indexAlert(alert);
                } catch (Exception retryEx) {
                    log.warn("告警写入 ES 重试仍失败: {} - {}", alert.getAlertCode(), retryEx.getMessage());
                }
            }

            // WebSocket 实时推送到前端
            webSocketHandler.broadcast(alert);
            log.warn("告警产生: {} - {} ({})", alert.getDeviceCode(), alert.getAlertType(), alert.getLevel());
        } catch (Exception e) {
            log.error("消费告警消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 消费任务下发消息 -> 模拟设备接收任务,更新任务状态
     */
    @KafkaListener(topics = "${kafka.topics.task-dispatch}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskDispatch(String message) {
        Map<String, Object> msg = parse(message);
        if (msg == null) return;
        try {
            String taskCode = (String) msg.get("taskCode");
            taskRepository.findByTaskCode(taskCode).ifPresent(task -> {
                task.setStatus("RUNNING");
                task.setDispatchTime(LocalDateTime.now());
                taskRepository.save(task);
            });
            log.info("任务指令已下发到设备: taskCode={}, deviceCode={}", taskCode, msg.get("deviceCode"));
        } catch (Exception e) {
            log.error("消费任务下发消息失败: {}", e.getMessage(), e);
        }
    }

    // ---------- 类型转换工具 ----------
    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        return Integer.parseInt(o.toString());
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(o.toString());
    }
}
