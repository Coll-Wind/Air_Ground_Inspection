package com.agi.service;

import com.agi.es.EsService;
import com.agi.model.Alert;
import com.agi.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private EsService esService;

    public List<Alert> listAll() {
        return alertRepository.findAll();
    }

    public List<Alert> listByType(String alertType) {
        return alertRepository.findByAlertType(alertType);
    }

    public List<Alert> listByDevice(String deviceCode) {
        return alertRepository.findByDeviceCode(deviceCode);
    }

    public List<Alert> listByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    /**
     * 多条件检索告警(走 Elasticsearch)
     */
    public List<Map<String, Object>> search(String deviceCode, String alertType,
                                            String level, String status) throws IOException {
        return esService.searchAlerts(deviceCode, alertType, level, status);
    }

    /**
     * 地理范围检索
     */
    public List<Map<String, Object>> searchByGeo(Double lat, Double lon, String distanceKm) throws IOException {
        return esService.searchByGeo(lat, lon, distanceKm);
    }

    /**
     * 按告警类型聚合统计
     */
    public Map<String, Long> aggregateByType() throws IOException {
        return esService.aggregateByAlertType();
    }

    /**
     * 处理告警(更新状态)
     */
    public Alert updateStatus(String id, String status) {
        return alertRepository.findById(id).map(alert -> {
            alert.setStatus(status);
            return alertRepository.save(alert);
        }).orElse(null);
    }

    public long count() {
        return alertRepository.count();
    }

    public long countByStatus(String status) {
        return alertRepository.findByStatus(status).size();
    }
}
