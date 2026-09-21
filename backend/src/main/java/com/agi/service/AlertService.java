package com.agi.service;

import com.agi.es.EsService;
import com.agi.model.Alert;
import com.agi.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /** 分页查询告警(按告警时间倒序),支持类型/状态/设备过滤 */
    public Map<String, Object> page(String type, String status, String deviceCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "alertTime"));
        Page<Alert> p;
        if (type != null) p = alertRepository.findByAlertType(type, pageable);
        else if (status != null) p = alertRepository.findByStatus(status, pageable);
        else if (deviceCode != null) p = alertRepository.findByDeviceCode(deviceCode, pageable);
        else p = alertRepository.findAll(pageable);
        return Map.of("list", p.getContent(), "total", p.getTotalElements());
    }

    public List<Alert> listByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    /**
     * 多条件检索告警(走 Elasticsearch,分页)
     */
    public Map<String, Object> search(String deviceCode, String alertType,
                                      String level, String status, int page, int size) throws IOException {
        return esService.searchAlerts(deviceCode, alertType, level, status, page, size);
    }

    /**
     * 地理范围检索
     */
    public Map<String, Object> searchByGeo(Double lat, Double lon, String distanceKm) throws IOException {
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
