package com.agi.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 设备实体 - 无人机/机器狗统一台账
 */
@Data
@Document(collection = "device")
public class Device {

    @Id
    private String id;

    /** 设备编号,如 UAV-001 / DOG-001 */
    private String deviceCode;

    /** 设备类型: DRONE(无人机) / ROBOT_DOG(机器狗) */
    private String deviceType;

    /** 设备名称 */
    private String name;

    /** 状态: ONLINE / OFFLINE / FAULT */
    private String status;

    /** 电量百分比 */
    private Integer battery;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 设备型号 */
    private String model;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 备注 */
    private String remark;
}
