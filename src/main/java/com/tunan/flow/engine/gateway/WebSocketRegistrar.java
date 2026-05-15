package com.tunan.flow.engine.gateway;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.engine.FlowExecutor;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.StandardWebSocketClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket 注册器
 * 支持内外网穿透（客户端模式主动连接）
 */
@Slf4j
@Component
public class WebSocketRegistrar {

    @Autowired
    private FlowExecutor flowExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    // 服务端 WebSocket 会话管理
    private final Map<String, List<WebSocketSession>> serverSessions = new ConcurrentHashMap<>();

    // 客户端 WebSocket 连接管理
    private final Map<String, WebSocketConnectionManager> clientConnections = new ConcurrentHashMap<>();

    // 流程ID到会话的映射
    public final Map<String, String> sessionToFlowId = new ConcurrentHashMap<>();

    /**
     * 注册 WebSocket
     */
    public void register(String flowId, String path, String wsMode,
                         String wsTargetUrl, FlowExecutor flowExecutor) {
        if ("client".equals(wsMode)) {
            // 客户端模式：主动连接外部服务器（内网穿透）
            connectAsClient(flowId, wsTargetUrl);
        } else {
            // 服务端模式：等待外部连接
            registerAsServer(flowId, path);
        }
        log.info("✅ WebSocket 注册成功: mode={}, path={}", wsMode, path);
    }

    /**
     * 服务端模式：注册 WebSocket 端点
     */
    private void registerAsServer(String flowId, String path) {
        // 注意：这里需要配合 WebSocketConfig 使用
        // 实际项目中，WebSocket 端点需要在配置类中预先注册
        // 这里记录映射关系，在 WebSocketHandler 中使用
        log.info("WebSocket 服务端模式: flowId={}, path={}", flowId, path);

        // 存储映射关系，供 WebSocketHandler 使用
        WebSocketPathRegistry.register(path, flowId);
    }

    /**
     * 客户端模式：主动连接外部服务器（实现内网穿透）
     */
    private void connectAsClient(String flowId, String wsTargetUrl) {
        try {
            // 创建 WebSocket 客户端
            StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

            // 创建连接管理器
            WebSocketConnectionManager manager = new WebSocketConnectionManager(
                    (WebSocketClient) webSocketClient,
                    new FlowWebSocketClientHandler(flowId, wsTargetUrl, flowExecutor, objectMapper),
                    wsTargetUrl
            );

            // 设置自动重连
            manager.setAutoStartup(true);
            manager.start();

            clientConnections.put(flowId, manager);

            log.info("WebSocket 客户端模式启动: flowId={}, target={}", flowId, wsTargetUrl);

        } catch (Exception e) {
            log.error("WebSocket 客户端连接失败: flowId={}, target={}", flowId, wsTargetUrl, e);
        }
    }

    /**
     * 推送消息到指定流程的所有客户端
     */
    public void broadcast(String flowId, String message) {
        List<WebSocketSession> sessions = serverSessions.get(flowId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("发送消息失败", e);
                    }
                }
            }
        }
    }

    /**
     * 取消注册
     */
    public void unregister(String path) {
        // 关闭所有服务端连接
        String flowId = WebSocketPathRegistry.getFlowId(path);
        if (flowId != null) {
            List<WebSocketSession> sessions = serverSessions.remove(flowId);
            if (sessions != null) {
                for (WebSocketSession session : sessions) {
                    try {
                        if (session.isOpen()) {
                            session.close();
                        }
                    } catch (IOException e) {
                        log.error("关闭会话失败", e);
                    }
                }
            }
        }

        // 关闭所有客户端连接
        WebSocketConnectionManager manager = clientConnections.remove(flowId);
        if (manager != null) {
            manager.stop();
        }

        WebSocketPathRegistry.unregister(path);
        log.info("WebSocket 取消注册: {}", path);
    }

    @PreDestroy
    public void destroy() {
        // 关闭所有客户端连接
        clientConnections.values().forEach(manager -> {
            try {
                manager.stop();
            } catch (Exception e) {
                log.error("关闭客户端连接失败", e);
            }
        });
        clientConnections.clear();
        serverSessions.clear();
    }

    /**
     * 获取服务端会话（供 WebSocketHandler 使用）
     */
    public void addSession(String flowId, WebSocketSession session) {
        serverSessions.computeIfAbsent(flowId, k -> new CopyOnWriteArrayList<>())
                .add(session);
        sessionToFlowId.put(session.getId(), flowId);
    }

    public void removeSession(String flowId, WebSocketSession session) {
        List<WebSocketSession> sessions = serverSessions.get(flowId);
        if (sessions != null) {
            sessions.remove(session);
        }
        sessionToFlowId.remove(session.getId());
    }
}
