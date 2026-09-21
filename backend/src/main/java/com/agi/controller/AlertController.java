package com.agi.controller;

import com.agi.model.Alert;
import com.agi.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @Autowired
    private AlertService alertService;

    /** 告警分页列表(按告警时间倒序,返回 {list, total}) */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(alertService.page(type, status, deviceCode, page, size));
    }

    /**
     * 多条件检索告警(走 Elasticsearch,分页)
     * 支持按设备、告警类型、级别、状态检索
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws IOException {
        return ResponseEntity.ok(alertService.search(deviceCode, alertType, level, status, page, size));
    }

    /**
     * 地理范围检索(基于 ES geo_point)
     */
    @GetMapping("/geo-search")
    public ResponseEntity<Map<String, Object>> geoSearch(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(defaultValue = "1") String distance) throws IOException {
        return ResponseEntity.ok(alertService.searchByGeo(lat, lon, distance));
    }

    /** 按告警类型聚合统计 */
    @GetMapping("/aggregate")
    public ResponseEntity<Map<String, Long>> aggregate() throws IOException {
        return ResponseEntity.ok(alertService.aggregateByType());
    }

    /** 更新告警处理状态 */
    @PutMapping("/{id}/status")
    public ResponseEntity<Alert> updateStatus(@PathVariable String id, @RequestParam String status) {
        Alert alert = alertService.updateStatus(id, status);
        return alert != null ? ResponseEntity.ok(alert) : ResponseEntity.notFound().build();
    }

    /** 告警统计 */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "total", alertService.count(),
                "pending", alertService.countByStatus("PENDING"),
                "processed", alertService.countByStatus("PROCESSED")
        ));
    }
}
