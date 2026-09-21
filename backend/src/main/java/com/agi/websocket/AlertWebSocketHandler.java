package com.agi.websocket;

import com.agi.auth.JwtUtil;
import com.agi.model.Alert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 告警 WebSocket 处理器 - 实时推送告警到前端
 * 连接握手时校验 JWT token(查询参数 token=xxx),未认证连接直接拒绝
 */
@Slf4j
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    /** 支持 LocalDateTime 序列化(否则告警时间字段会让广播失败) */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!authenticate(session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            log.warn("WebSocket 认证失败,拒绝连接: {}", session.getId());
            return;
        }
        sessions.add(session);
        log.info("WebSocket 连接建立(已认证): {}", session.getId());
    }

    /** 从握手 URL 查询参数中解析并校验 JWT */
    private boolean authenticate(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null || uri.getQuery() == null) return false;
            for (String p : uri.getQuery().split("&")) {
                String[] kv = p.split("=", 2);
                if ("token".equals(kv[0]) && kv.length == 2) {
                    // verify 校验失败会抛异常,由外层 catch 捕获返回 false
                    return jwtUtil.verify(kv[1]) != null;
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket token 校验异常: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 连接关闭: {}", session.getId());
    }

    /**
     * 广播告警到所有已连接的前端
     * 并发场景下同一 session 的 sendMessage 必须串行,用会话级锁保护
     */
    public void broadcast(Alert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);
            TextMessage message = new TextMessage(json);
            sessions.removeIf(s -> !s.isOpen());
            for (WebSocketSession session : sessions) {
                synchronized (session) {
                    try {
                        session.sendMessage(message);
                    } catch (Exception se) {
                        log.warn("推送到会话 {} 失败: {}", session.getId(), se.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("WebSocket 广播告警失败: {}", e.getMessage());
        }
    }
}
