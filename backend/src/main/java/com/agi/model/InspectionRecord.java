package com.agi.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 巡检记录实体 - 设备上报的原始巡检数据
 */
@Data
@Document(collection = "inspection_record")
public class InspectionRecord {

    @Id
    private String id;

    /** 记录编号 */
    private String recordCode;

    /** 设备编号 */
    private String deviceCode;

    /** 设备类型 */
    private String deviceType;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 设备数据(无人机:航拍元数据/高度;机器狗:红外温度/传感器数据) */
    private String payload;

    /** 巡检图片在 HDFS 的路径 */
    private String imagePath;

    /** 上报时间 */
    private LocalDateTime reportTime;
}
