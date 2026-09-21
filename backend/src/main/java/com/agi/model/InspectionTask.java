package com.agi.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 巡检任务实体
 */
@Data
@Document(collection = "task")
public class InspectionTask {

    @Id
    private String id;

    /** 任务编号 */
    private String taskCode;

    /** 任务名称 */
    private String name;

    /** 目标设备编号 */
    private String deviceCode;

    /** 任务类型: PATROL(巡逻) / INSPECT(巡检) / ALERT_CHECK(告警复核) */
    private String taskType;

    /** 任务状态: PENDING / RUNNING / COMPLETED / FAILED */
    private String status;

    /** 巡检区域描述 */
    private String area;

    /** 目标纬度 */
    private Double latitude;

    /** 目标经度 */
    private Double longitude;

    /** 任务描述 */
    private String description;

    /** 下发时间 */
    private LocalDateTime dispatchTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
