package com.tunan.flow.engine.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.engine.FlowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;


/**
 * WebSocket 服务端处理器（等待连接模式）
 */
@Slf4j
@Component
public class FlowWebSocketServerHandler extends AbstractWebSocketHandler {

    @Autowired
    private WebSocketRegistrar webSocketRegistrar;

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri().getPath();
        String flowId = WebSocketPathRegistry.getFlowId(path);

        if (flowId != null) {
            webSocketRegistrar.addSession(flowId, session);
            log.info("WebSocket 服务端连接建立: flowId={}, sessionId={}", flowId, session.getId());
        } else {
            log.warn("未找到对应的流程: path={}", path);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String flowId = webSocketRegistrar.sessionToFlowId.get(session.getId());
        if (flowId == null) {
            return;
        }

        try {
            String payload = message.getPayload();
            Map<String, Object> request = objectMapper.readValue(payload, Map.class);

            // 执行流程
            Object result = flowExecutor.execute(flowId, request).getResult();

            // 发送响应
            Map<String, Object> response = Map.of(
                    "type", "response",
                    "data", result,
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } catch (Exception e) {
            log.error("处理消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String flowId = webSocketRegistrar.sessionToFlowId.get(session.getId());
        if (flowId != null) {
            webSocketRegistrar.removeSession(flowId, session);
            log.info("WebSocket 服务端连接关闭: flowId={}, sessionId={}", flowId, session.getId());
        }
    }
}
