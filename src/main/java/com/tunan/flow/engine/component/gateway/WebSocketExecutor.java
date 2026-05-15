package com.tunan.flow.engine.component.gateway;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * WebSocket执行器
 * 支持双向通信，可实现内外网穿透
 */
@Slf4j
@Component
public class WebSocketExecutor implements ComponentExecutor {
    @Override
    public String getType() {
        return "websocket-api";
    }

    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        context.setVariable("_gatewayConfig", config);
        context.setVariable("_protocol", "websocket");
        context.setVariable("_wsMode", config.getOrDefault("wsMode", "server"));

        log.debug("WebSocket执行器: mode={}, path={}",
                config.get("wsMode"), config.get("path"));

        return input;
    }
}
