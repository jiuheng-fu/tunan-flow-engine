package com.tunan.flow.engine.component.gateway;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Function Calling执行器
 * 将流程发布为 OpenAI Function Calling 工具
 */
@Slf4j
@Component
public class FunctionCallExecutor implements ComponentExecutor {

    @Override
    public String getType() {
        return "function-call";
    }

    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        context.setVariable("_gatewayConfig", config);
        context.setVariable("_protocol", "function");
        context.setVariable("_functionName", config.get("functionName"));

        log.debug("Function Calling执行器: functionName={}", config.get("functionName"));

        return input;
    }

    /**
     * 获取函数定义（用于 OpenAI Function Calling）
     */
    public Map<String, Object> getFunctionDefinition(Map<String, Object> config) {
        return Map.of(
                "name", config.get("functionName"),
                "description", config.get("functionDescription"),
                "parameters", config.getOrDefault("parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                ))
        );
    }
}
