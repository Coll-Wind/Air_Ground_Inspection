package com.agi.simulator;

import com.agi.hdfs.HdfsService;
import com.agi.kafka.KafkaProducerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备仿真器 - 模拟无人机/机器狗设备注册、心跳、数据上报、告警
 * 通过 @Scheduled 定时向 Kafka 生产消息,模拟真实设备数据流
 */
@Slf4j
@Component
public class DeviceSimulator {

    @Autowired
    private KafkaProducerService kafkaProducer;
    @Autowired
    private HdfsService hdfsService;

    @Value("${simulator.enabled:true}")
    private boolean enabled;

    @Value("${simulator.drone-count:3}")
    private int droneCount;

    @Value("${simulator.robot-dog-count:2}")
    private int robotDogCount;

    // 设备运行状态内存表
    private final Map<String, DeviceState> devices = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // 园区坐标范围(模拟某园区)
    private static final double BASE_LAT = 39.9042;
    private static final double BASE_LON = 116.4074;

    private static final String[] ALERT_TYPES = {"OVERHEAT", "INTRUSION", "SMOKE", "LOW_BATTERY", "DEVICE_FAULT"};
    private static final String[] ALERT_LEVELS = {"HIGH", "MEDIUM", "LOW"};

    @PostConstruct
    public void init() {
        if (!enabled) return;
        // 初始化无人机
        for (int i = 1; i <= droneCount; i++) {
            String code = "UAV-" + String.format("%03d", i);
            devices.put(code, new DeviceState(code, "DRONE", "大疆 Mavic 3", randomLat(), randomLon(), 100));
        }
        // 初始化机器狗
        for (int i = 1; i <= robotDogCount; i++) {
            String code = "DOG-" + String.format("%03d", i);
            devices.put(code, new DeviceState(code, "ROBOT_DOG", "Unitree Go2", randomLat(), randomLon(), 100));
        }
        // 启动时注册所有设备
        devices.values().forEach(this::registerDevice);
        log.info("设备仿真器初始化完成: 无人机 {} 台, 机器狗 {} 台", droneCount, robotDogCount);
    }

    // ==================== 定时任务 ====================

    /**
     * 心跳上报:每 30 秒所有设备发送心跳
     */
    @Scheduled(fixedDelayString = "${simulator.heartbeat-interval:30000}", initialDelay = 10000)
    public void sendHeartbeats() {
        if (!enabled) return;
        for (DeviceState ds : devices.values()) {
            // 电量缓慢下降
            ds.battery = Math.max(5, ds.battery - random.nextInt(3));
            // 位置小幅移动
            ds.latitude += (random.nextDouble() - 0.5) * 0.001;
            ds.longitude += (random.nextDouble() - 0.5) * 0.001;

            Map<String, Object> msg = baseMessage(ds);
            msg.put("battery", ds.battery);
            kafkaProducer.sendHeartbeat(msg);
        }
        log.debug("批量心跳上报完成,共 {} 台设备", devices.size());
    }

    /**
     * 巡检数据上报:每 15 秒各设备上报一次巡检数据
     */
    @Scheduled(fixedDelayString = "${simulator.data-interval:15000}", initialDelay = 5000)
    public void sendInspectionData() {
        if (!enabled) return;
        for (DeviceState ds : devices.values()) {
            Map<String, Object> msg = baseMessage(ds);
            // 无人机:高度、航拍图片;机器狗:红外温度、传感器
            String payload;
            if ("DRONE".equals(ds.deviceType)) {
                payload = String.format("{\"altitude\":%d,\"speed\":%.1f,\"cameraAngle\":%d}",
                        50 + random.nextInt(100), 5 + random.nextDouble() * 10, random.nextInt(360));
            } else {
                payload = String.format("{\"temperature\":%.1f,\"humidity\":%d,\"gas\":\"%.2f\"}",
                        20 + random.nextDouble() * 15, 30 + random.nextInt(50), random.nextDouble() * 0.5);
            }
            msg.put("payload", payload);

            // 模拟上传一张巡检图片到 HDFS
            try {
                String imagePath = uploadMockImage(ds.deviceCode);
                msg.put("imagePath", imagePath);
            } catch (Exception e) {
                msg.put("imagePath", "");
                log.warn("仿真图片上传 HDFS 失败: {}", e.getMessage());
            }
            kafkaProducer.sendDeviceData(msg);
        }
    }

    /**
     * 告警上报:每 60 秒随机产生一条告警
     */
    @Scheduled(fixedDelayString = "${simulator.alert-interval:60000}", initialDelay = 20000)
    public void sendAlert() {
        if (!enabled || devices.isEmpty()) return;
        // 随机选一台设备
        List<DeviceState> list = new ArrayList<>(devices.values());
        DeviceState ds = list.get(random.nextInt(list.size()));

        String alertType = ALERT_TYPES[random.nextInt(ALERT_TYPES.length)];
        String level = "LOW_BATTERY".equals(alertType) ? "MEDIUM" : ALERT_LEVELS[random.nextInt(ALERT_LEVELS.length)];

        Map<String, Object> msg = baseMessage(ds);
        msg.put("alertType", alertType);
        msg.put("level", level);
        msg.put("description", buildAlertDescription(alertType, ds));
        kafkaProducer.sendAlert(msg);
    }

    // ==================== 内部方法 ====================

    private void registerDevice(DeviceState ds) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("deviceCode", ds.deviceCode);
        msg.put("deviceType", ds.deviceType);
        msg.put("name", ds.deviceType.equals("DRONE") ? "巡检无人机-" + ds.deviceCode : "巡检机器狗-" + ds.deviceCode);
        msg.put("model", ds.model);
        msg.put("battery", ds.battery);
        msg.put("latitude", ds.latitude);
        msg.put("longitude", ds.longitude);
        msg.put("registerTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        kafkaProducer.sendDeviceRegister(msg);
        log.info("仿真设备注册: {} ({})", ds.deviceCode, ds.model);
    }

    /**
     * 模拟上传一张巡检图片到 HDFS(生成一个小的字节数组模拟图片)
     */
    private String uploadMockImage(String deviceCode) throws Exception {
        // 生成 1KB 模拟图片数据(实际项目中为真实图片字节流)
        byte[] mockImage = new byte[1024];
        random.nextBytes(mockImage);
        return hdfsService.uploadFile(new ByteArrayInputStream(mockImage), deviceCode, "inspection.jpg");
    }

    private Map<String, Object> baseMessage(DeviceState ds) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("deviceCode", ds.deviceCode);
        msg.put("deviceType", ds.deviceType);
        msg.put("latitude", ds.latitude);
        msg.put("longitude", ds.longitude);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    private String buildAlertDescription(String type, DeviceState ds) {
        return switch (type) {
            case "OVERHEAT" -> ds.deviceCode + " 检测到设备温度过高,需立即降温";
            case "INTRUSION" -> ds.deviceCode + " 在巡检区域发现可疑人员入侵";
            case "SMOKE" -> ds.deviceCode + " 检测到烟雾,疑似火情";
            case "LOW_BATTERY" -> ds.deviceCode + " 电量不足(" + ds.battery + "%),请及时充电";
            case "DEVICE_FAULT" -> ds.deviceCode + " 设备运行异常,需要检修";
            default -> "未知告警";
        };
    }

    private double randomLat() {
        return BASE_LAT + (random.nextDouble() - 0.5) * 0.05;
    }

    private double randomLon() {
        return BASE_LON + (random.nextDouble() - 0.5) * 0.05;
    }

    /** 设备运行状态内存模型 */
    private static class DeviceState {
        String deviceCode;
        String deviceType;
        String model;
        double latitude;
        double longitude;
        int battery;

        DeviceState(String deviceCode, String deviceType, String model, double latitude, double longitude, int battery) {
            this.deviceCode = deviceCode;
            this.deviceType = deviceType;
            this.model = model;
            this.latitude = latitude;
            this.longitude = longitude;
            this.battery = battery;
        }
    }
}
