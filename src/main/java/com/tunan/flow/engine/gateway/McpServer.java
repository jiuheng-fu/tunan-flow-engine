package com.tunan.flow.engine.gateway;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.engine.FlowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server - 实现 Model Context Protocol
 * 用于与 Claude 等 AI 工具集成
 */
@Slf4j
@Component
public class McpServer {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 注册 MCP 工具
     */
    public void registerTool(String flowId, Map<String, Object> config, FlowExecutor flowExecutor) {
        String toolName = (String) config.get("toolName");

        ToolDefinition tool = ToolDefinition.builder()
                .name(toolName)
                .description((String) config.get("toolDescription"))
                .inputSchema((Map<String, Object>) config.getOrDefault("inputSchema", Map.of()))
                .flowId(flowId)
                .flowExecutor(flowExecutor)
                .build();

        tools.put(toolName, tool);

        log.info("✅ MCP Tool 注册成功: {} -> flowId: {}", toolName, flowId);
    }

    /**
     * 取消注册工具
     */
    public void unregisterTool(String toolName) {
        tools.remove(toolName);
        log.info("MCP Tool 取消注册: {}", toolName);
    }

    /**
     * 获取所有工具列表
     */
    public Map<String, ToolDefinition> getTools() {
        return tools;
    }

    /**
     * 获取工具定义
     */
    public ToolDefinition getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * 创建 SSE 会话（MCP 连接）
     */
    public SseEmitter createSession() {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));

        sessions.put(sessionId, emitter);

        // 发送 endpoint 事件
        try {
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/mcp/messages?sessionId=" + sessionId));
        } catch (IOException e) {
            log.error("发送 endpoint 失败", e);
        }

        return emitter;
    }

    /**
     * 执行工具调用
     */
    public Object executeTool(String toolName, Map<String, Object> arguments, String sessionId) {
        ToolDefinition tool = tools.get(toolName);
        if (tool == null) {
            throw new RuntimeException("Tool not found: " + toolName);
        }

        // 执行流程
        Object result = tool.getFlowExecutor()
                .execute(tool.getFlowId(), arguments)
                .getResult();

        // 如果 SSE 会话存在，推送结果
        SseEmitter emitter = sessions.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(objectMapper.writeValueAsString(Map.of(
                                "jsonrpc", "2.0",
                                "result", Map.of("content", result)
                        ))));
            } catch (IOException e) {
                log.error("推送结果失败", e);
            }
        }

        return result;
    }

    /**
     * 工具定义
     */
    @lombok.Builder
    @lombok.Data
    public static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
        private String flowId;
        private FlowExecutor flowExecutor;
    }
}
