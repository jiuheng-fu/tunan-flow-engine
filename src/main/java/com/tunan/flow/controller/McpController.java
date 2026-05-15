package com.tunan.flow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.engine.gateway.McpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * MCP 协议控制器
 * 用于与 Claude Desktop、Cursor 等 AI 工具集成
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpServer mcpServer;
    private final ObjectMapper objectMapper;

    /**
     * SSE 连接端点（Claude 连接用）
     * GET /mcp/sse
     */
    @GetMapping(value = "/sse", produces = "text/event-stream")
    public SseEmitter connect() {
        log.info("MCP SSE 连接建立");
        return mcpServer.createSession();
    }

    /**
     * 消息接收端点（Claude 发送请求用）
     * POST /mcp/messages?sessionId=xxx
     */
    @PostMapping("/messages")
    public Map<String, Object> handleMessage(
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> request) {

        log.debug("MCP 消息: sessionId={}, request={}", sessionId, request);

        String method = (String) request.get("method");

        // 处理 tools/list 请求
        if ("tools/list".equals(method)) {
            return Map.of(
                    "jsonrpc", "2.0",
                    "id", request.get("id"),
                    "result", Map.of("tools", mcpServer.getTools().values().stream()
                            .map(tool -> Map.of(
                                    "name", tool.getName(),
                                    "description", tool.getDescription(),
                                    "inputSchema", tool.getInputSchema()
                            ))
                            .toList())
            );
        }

        // 处理 tools/call 请求
        if ("tools/call".equals(method)) {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

            Object result = mcpServer.executeTool(toolName, arguments, sessionId);

            return Map.of(
                    "jsonrpc", "2.0",
                    "id", request.get("id"),
                    "result", Map.of("content", result)
            );
        }

        return Map.of(
                "jsonrpc", "2.0",
                "id", request.get("id"),
                "error", Map.of("code", -32601, "message", "Method not found")
        );
    }

    /**
     * 获取所有工具列表（用于调试）
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return Map.of("tools", mcpServer.getTools().values().stream()
                .map(tool -> Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "inputSchema", tool.getInputSchema()
                ))
                .toList());
    }
}
