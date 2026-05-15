package com.tunan.flow.engine.component.gateway;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * MCP工具执行器
 * 将流程发布为 MCP Tool，供 Claude 等 AI 调用
 */
@Slf4j
@Component
public class McpToolExecutor implements ComponentExecutor {
    @Override
    public String getType() {
        return "mcp-tool";
    }

    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        context.setVariable("_gatewayConfig", config);
        context.setVariable("_protocol", "mcp");
        context.setVariable("_toolName", config.get("toolName"));

        log.debug("MCP工具执行器: toolName={}, description={}",
                config.get("toolName"), config.get("toolDescription"));

        return input;
    }

    /**
     * 获取工具定义（用于 MCP 协议发现）
     */
    public Map<String, Object> getToolDefinition(Map<String, Object> config) {
        return Map.of(
                "name", config.get("toolName"),
                "description", config.get("toolDescription"),
                "inputSchema", config.getOrDefault("inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                ))
        );
    }

}
