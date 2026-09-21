package com.agi.service;

import com.agi.kafka.KafkaProducerService;
import com.agi.model.InspectionTask;
import com.agi.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private KafkaProducerService kafkaProducer;

    /**
     * 创建并下发巡检任务
     */
    public InspectionTask createAndDispatch(InspectionTask task) {
        task.setId(UUID.randomUUID().toString());
        task.setTaskCode("TASK-" + System.currentTimeMillis());
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        InspectionTask saved = taskRepository.save(task);

        // 通过 Kafka 下发任务指令到仿真设备
        Map<String, Object> msg = new HashMap<>();
        msg.put("taskCode", saved.getTaskCode());
        msg.put("deviceCode", saved.getDeviceCode());
        msg.put("taskType", saved.getTaskType());
        msg.put("area", saved.getArea());
        msg.put("latitude", saved.getLatitude());
        msg.put("longitude", saved.getLongitude());
        msg.put("description", saved.getDescription());
        kafkaProducer.sendTaskDispatch(msg);

        return saved;
    }

    /** 分页查询任务(按创建时间倒序),支持状态/设备过滤 */
    public Map<String, Object> page(String status, String deviceCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<InspectionTask> p;
        if (status != null) p = taskRepository.findByStatus(status, pageable);
        else if (deviceCode != null) p = taskRepository.findByDeviceCode(deviceCode, pageable);
        else p = taskRepository.findAll(pageable);
        return Map.of("list", p.getContent(), "total", p.getTotalElements());
    }

    /**
     * 任务完成(仿真设备执行完毕回调)
     */
    public InspectionTask complete(String taskCode) {
        return taskRepository.findByTaskCode(taskCode).map(task -> {
            // 状态机校验:仅 RUNNING 状态允许标记完成
            if (!"RUNNING".equals(task.getStatus())) {
                throw new IllegalArgumentException("任务 " + taskCode + " 当前状态为 " + task.getStatus() + ",不允许标记完成");
            }
            task.setStatus("COMPLETED");
            task.setCompleteTime(LocalDateTime.now());
            return taskRepository.save(task);
        }).orElse(null);
    }
}
