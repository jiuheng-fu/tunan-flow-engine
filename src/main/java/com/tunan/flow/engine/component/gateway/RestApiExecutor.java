package com.tunan.flow.engine.component.gateway;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * REST API执行器
 * 支持 GET/POST/PUT/DELETE 方法
 */
@Slf4j
@Component
public class RestApiExecutor implements ComponentExecutor {
    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        context.setVariable("_gatewayConfig", config);
        context.setVariable("_protocol", "rest");

        log.debug("REST API执行器: method={}, path={}",
                config.get("method"), config.get("path"));

        return input;
    }

    @Override
    public String getType() {
        return "rest-api";
    }
}
