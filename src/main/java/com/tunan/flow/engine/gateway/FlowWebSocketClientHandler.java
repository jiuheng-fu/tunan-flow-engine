package com.tunan.flow.engine.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.engine.FlowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;


/**
 * WebSocket 客户端处理器（主动连接模式）
 */
@Slf4j
public class FlowWebSocketClientHandler extends AbstractWebSocketHandler {

    private final String flowId;
    private final String wsTargetUrl;
    private final FlowExecutor flowExecutor;
    private final ObjectMapper objectMapper;
    private WebSocketSession session;

    public FlowWebSocketClientHandler(String flowId, String wsTargetUrl,
                                      FlowExecutor flowExecutor, ObjectMapper objectMapper) {
        this.flowId = flowId;
        this.wsTargetUrl = wsTargetUrl;
        this.flowExecutor = flowExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        log.info("WebSocket 客户端连接成功: flowId={}, target={}", flowId, wsTargetUrl);

        // 发送注册消息
        try {
            Map<String, Object> registerMsg = Map.of(
                    "type", "register",
                    "flowId", flowId,
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(registerMsg)));
        } catch (Exception e) {
            log.error("发送注册消息失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("WebSocket 收到消息: flowId={}, message={}", flowId, payload);

            // 解析消息
            Map<String, Object> request = objectMapper.readValue(payload, Map.class);
            String type = (String) request.get("type");

            if ("execute".equals(type)) {
                // 执行流程
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                Object result = flowExecutor.execute(flowId, params).getResult();

                // 发送响应
                Map<String, Object> response = Map.of(
                        "type", "result",
                        "requestId", request.get("id"),
                        "data", result,
                        "timestamp", System.currentTimeMillis()
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }

        } catch (Exception e) {
            log.error("处理消息失败", e);
            try {
                session.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(Map.of("type", "error", "message", e.getMessage()))
                ));
            } catch (IOException ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输错误: flowId={}", flowId, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 连接关闭: flowId={}, status={}", flowId, status);
        this.session = null;

        // 尝试重连
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        // 延迟5秒后重连
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                log.info("尝试重连 WebSocket: flowId={}", flowId);
                // 重新连接（由外部触发）
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

}
