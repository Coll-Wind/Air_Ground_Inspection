package com.agi.controller;

import com.agi.model.InspectionTask;
import com.agi.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /** 任务列表(可按状态/设备筛选) */
    @GetMapping
    public ResponseEntity<List<InspectionTask>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceCode) {
        if (status != null) return ResponseEntity.ok(taskService.listByStatus(status));
        if (deviceCode != null) return ResponseEntity.ok(taskService.listByDevice(deviceCode));
        return ResponseEntity.ok(taskService.listAll());
    }

    /** 创建并下发巡检任务 */
    @PostMapping
    public ResponseEntity<InspectionTask> create(@RequestBody InspectionTask task) {
        return ResponseEntity.ok(taskService.createAndDispatch(task));
    }

    /** 标记任务完成 */
    @PutMapping("/{taskCode}/complete")
    public ResponseEntity<InspectionTask> complete(@PathVariable String taskCode) {
        InspectionTask task = taskService.complete(taskCode);
        return task != null ? ResponseEntity.ok(task) : ResponseEntity.notFound().build();
    }
}
