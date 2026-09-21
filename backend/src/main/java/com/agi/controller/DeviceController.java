package com.agi.controller;

import com.agi.model.Device;
import com.agi.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    /** 设备列表(可按类型/状态筛选) */
    @GetMapping
    public ResponseEntity<List<Device>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        if (type != null) return ResponseEntity.ok(deviceService.listByType(type));
        if (status != null) return ResponseEntity.ok(deviceService.listByStatus(status));
        return ResponseEntity.ok(deviceService.listAll());
    }

    /** 设备详情 */
    @GetMapping("/{code}")
    public ResponseEntity<Device> get(@PathVariable String code) {
        return deviceService.getByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 设备统计 */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", deviceService.count());
        stats.put("online", deviceService.countByStatus("ONLINE"));
        stats.put("offline", deviceService.countByStatus("OFFLINE"));
        stats.put("fault", deviceService.countByStatus("FAULT"));
        return ResponseEntity.ok(stats);
    }

    /** 删除设备 */
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        deviceService.deleteByCode(code);
        return ResponseEntity.noContent().build();
    }
}
