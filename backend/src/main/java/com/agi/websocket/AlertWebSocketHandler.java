package com.agi.websocket;

import com.agi.model.Alert;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 告警 WebSocket 处理器 - 实时推送告警到前端
 */
@Slf4j
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket 连接关闭: {}", session.getId());
    }

    /**
     * 广播告警到所有已连接的前端
     */
    public void broadcast(Alert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);
            TextMessage message = new TextMessage(json);
            sessions.removeIf(s -> !s.isOpen());
            for (WebSocketSession session : sessions) {
                session.sendMessage(message);
            }
        } catch (Exception e) {
            log.error("WebSocket 广播告警失败: {}", e.getMessage());
        }
    }
}
