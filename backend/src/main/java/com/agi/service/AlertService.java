package com.agi.service;

import com.agi.es.EsService;
import com.agi.model.Alert;
import com.agi.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private EsService esService;
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 分页查询告警(按告警时间倒序),支持多条件组合过滤:
     * 类型/状态/设备/级别精确匹配,编号与描述关键词模糊匹配(忽略大小写)
     */
    public Map<String, Object> page(String type, String status, String deviceCode, String level,
                                    String alertCode, String keyword, int page, int size) {
        List<Criteria> parts = new ArrayList<>();
        if (type != null && !type.isEmpty()) parts.add(Criteria.where("alertType").is(type));
        if (status != null && !status.isEmpty()) parts.add(Criteria.where("status").is(status));
        if (deviceCode != null && !deviceCode.isEmpty()) parts.add(Criteria.where("deviceCode").regex(Pattern.quote(deviceCode), "i"));
        if (level != null && !level.isEmpty()) parts.add(Criteria.where("level").is(level));
        if (alertCode != null && !alertCode.isEmpty()) parts.add(Criteria.where("alertCode").regex(Pattern.quote(alertCode), "i"));
        if (keyword != null && !keyword.isEmpty()) parts.add(Criteria.where("description").regex(Pattern.quote(keyword), "i"));

        Query query = parts.isEmpty() ? new Query(new Criteria()) : new Query(new Criteria().andOperator(parts));
        long total = mongoTemplate.count(query, Alert.class);
        query.with(Sort.by(Sort.Direction.DESC, "alertTime"))
                .skip((long) Math.max(page - 1, 0) * size)
                .limit(size);
        List<Alert> list = mongoTemplate.find(query, Alert.class);
        return Map.of("list", list, "total", total);
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
     * 处理告警(更新状态,记录处理人/备注/时间),并同步更新 ES 保证双写一致
     */
    public Alert updateStatus(String id, String status, String handler, String remark) {
        Alert alert = alertRepository.findById(id).map(a -> {
            a.setStatus(status);
            if (handler != null && !handler.isEmpty()) a.setHandler(handler);
            if (remark != null && !remark.isEmpty()) a.setRemark(remark);
            a.setHandleTime(java.time.LocalDateTime.now());
            return alertRepository.save(a);
        }).orElse(null);
        if (alert != null) {
            try {
                esService.updateAlertStatus(id, status, alert.getHandler(), alert.getRemark(),
                        alert.getHandleTime() == null ? null : alert.getHandleTime().toString());
            } catch (Exception e) {
                log.warn("告警 {} 处理状态同步 ES 失败: {}", alert.getAlertCode(), e.getMessage());
            }
        }
        return alert;
    }

    public long count() {
        return alertRepository.count();
    }

    public long countByStatus(String status) {
        return alertRepository.findByStatus(status).size();
    }
}
