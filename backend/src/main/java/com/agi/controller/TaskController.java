package com.agi.controller;

import com.agi.model.InspectionTask;
import com.agi.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /** 任务分页列表(按创建时间倒序,返回 {list, total}) */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(taskService.page(status, deviceCode, page, size));
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
