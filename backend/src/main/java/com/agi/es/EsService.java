package com.agi.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.agi.model.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 检索服务 - 告警事件索引/检索/统计
 */
@Slf4j
@Service
public class EsService {

    public static final String ALERT_INDEX = "alert-index";

    @Autowired
    private ElasticsearchClient esClient;

    /**
     * 确保索引存在(含 geo_point 映射)
     */
    public void ensureIndex() throws IOException {
        if (!esClient.indices().exists(e -> e.index(ALERT_INDEX)).value()) {
            esClient.indices().create(c -> c
                    .index(ALERT_INDEX)
                    .mappings(m -> m
                            .properties("location", p -> p.geoPoint(g -> g))
                            .properties("alertType", p -> p.keyword(k -> k))
                            .properties("level", p -> p.keyword(k -> k))
                            .properties("deviceCode", p -> p.keyword(k -> k))
                            .properties("status", p -> p.keyword(k -> k))
                            .properties("alertTime", p -> p.date(d -> d))
                    )
            );
            log.info("ES 索引 {} 创建成功", ALERT_INDEX);
        }
    }

    /**
     * 同步更新 ES 中告警的处理状态/处理人/备注/处理时间(按 Mongo 文档 id)
     */
    public void updateAlertStatus(String id, String status, String handler, String remark, String handleTime) throws IOException {
        Map<String, Object> doc = new HashMap<>();
        doc.put("status", status);
        if (handler != null) doc.put("handler", handler);
        if (remark != null) doc.put("remark", remark);
        if (handleTime != null) doc.put("handleTime", handleTime);
        esClient.update(u -> u.index(ALERT_INDEX).id(id).doc(doc), Map.class);
    }

    /**
     * 索引一条告警
     */
    public String indexAlert(Alert alert) throws IOException {
        Map<String, Object> doc = new HashMap<>();
        doc.put("alertCode", alert.getAlertCode());
        doc.put("deviceCode", alert.getDeviceCode());
        doc.put("deviceType", alert.getDeviceType());
        doc.put("alertType", alert.getAlertType());
        doc.put("level", alert.getLevel());
        doc.put("description", alert.getDescription());
        doc.put("status", alert.getStatus());
        doc.put("alertTime", alert.getAlertTime().toString());
        // ES geo_point 格式
        Map<String, Double> location = new HashMap<>();
        location.put("lat", alert.getLatitude());
        location.put("lon", alert.getLongitude());
        doc.put("location", location);

        IndexResponse response = esClient.index(i -> i
                .index(ALERT_INDEX)
                .id(alert.getId())
                .document(doc)
        );
        return response.id();
    }

    /**
     * 多条件检索告警(分页,按告警时间倒序)
     * @return {list: 命中文档列表, total: 命中总数}
     */
    public Map<String, Object> searchAlerts(String deviceCode, String alertType,
                                            String level, String status, int page, int size) throws IOException {
        List<Query> mustQueries = new ArrayList<>();

        if (deviceCode != null && !deviceCode.isEmpty()) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("deviceCode").value(deviceCode))));
        }
        if (alertType != null && !alertType.isEmpty()) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("alertType").value(alertType))));
        }
        if (level != null && !level.isEmpty()) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("level").value(level))));
        }
        if (status != null && !status.isEmpty()) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("status").value(status))));
        }

        Query finalQuery = mustQueries.isEmpty()
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(b -> b.must(mustQueries)));

        SearchResponse<Map> response = esClient.search(s -> s
                        .index(ALERT_INDEX)
                        .query(finalQuery)
                        .from(Math.max(page - 1, 0) * size)
                        .size(size)
                        .sort(sort -> sort.field(f -> f.field("alertTime").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                Map.class);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            result.add(hit.source());
        }
        long total = response.hits().total() != null ? response.hits().total().value() : result.size();
        return Map.of("list", result, "total", total);
    }

    /**
     * 地理范围检索:查询指定经纬度周围指定半径内的告警
     * @return {list, total}
     */
    public Map<String, Object> searchByGeo(Double lat, Double lon, String distanceKm) throws IOException {
        SearchResponse<Map> response = esClient.search(s -> s
                        .index(ALERT_INDEX)
                        .query(q -> q.geoDistance(g -> g
                                .field("location")
                                .distance(distanceKm + "km")
                                .location(GeoLocation.of(l -> l.latlon(ll -> ll.lat(lat).lon(lon))))))
                        .size(200),
                Map.class);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            result.add(hit.source());
        }
        return Map.of("list", result, "total", (long) result.size());
    }

    /**
     * 按告警类型聚合统计
     */
    public Map<String, Long> aggregateByAlertType() throws IOException {
        SearchResponse<Map> response = esClient.search(s -> s
                        .index(ALERT_INDEX)
                        .size(0)
                        .aggregations("byType", a -> a.terms(t -> t.field("alertType").size(20))),
                Map.class);

        Map<String, Long> result = new HashMap<>();
        response.aggregations().get("byType").sterms().buckets().array().forEach(bucket -> {
            result.put(bucket.key().stringValue(), bucket.docCount());
        });
        return result;
    }
}
