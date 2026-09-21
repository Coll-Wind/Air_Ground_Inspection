package com.agi.service;

import com.agi.kafka.KafkaProducerService;
import com.agi.model.InspectionTask;
import com.agi.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<InspectionTask> listAll() {
        return taskRepository.findAll();
    }

    public List<InspectionTask> listByStatus(String status) {
        return taskRepository.findByStatus(status);
    }

    public List<InspectionTask> listByDevice(String deviceCode) {
        return taskRepository.findByDeviceCode(deviceCode);
    }

    /**
     * 任务完成(仿真设备执行完毕回调)
     */
    public InspectionTask complete(String taskCode) {
        return taskRepository.findByTaskCode(taskCode).map(task -> {
            task.setStatus("COMPLETED");
            task.setCompleteTime(LocalDateTime.now());
            return taskRepository.save(task);
        }).orElse(null);
    }
}
