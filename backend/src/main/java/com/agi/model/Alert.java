package com.agi.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 告警事件实体 - 同时写入 MongoDB 和 Elasticsearch
 */
@Data
@Document(collection = "alert")
public class Alert {

    @Id
    private String id;

    /** 告警编号 */
    private String alertCode;

    /** 设备编号 */
    private String deviceCode;

    /** 设备类型 */
    private String deviceType;

    /** 告警类型: OVERHEAT(过热) / INTRUSION(入侵) / SMOKE(烟雾) / LOW_BATTERY(低电量) / DEVICE_FAULT(设备故障) */
    private String alertType;

    /** 告警级别: HIGH / MEDIUM / LOW */
    private String level;

    /** 告警描述 */
    private String description;

    /** 告警位置 - 纬度 */
    private Double latitude;

    /** 告警位置 - 经度 */
    private Double longitude;

    /** 处理状态: PENDING / PROCESSED / IGNORED */
    private String status;

    /** 告警时间 */
    private LocalDateTime alertTime;

    /** 关联巡检图片在 HDFS 的路径 */
    private String imagePath;
}
