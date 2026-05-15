package com.tunan.flow.engine.component.gateway;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * API网关执行器 - 统一入口组件
 * 不执行具体业务逻辑，只负责存储配置信息
 * 实际的路由注册由 GatewayRegistrar 在发布时完成
 */
@Slf4j
@Component
public class ApiGatewayExecutor implements ComponentExecutor {
    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        // 入口组件不执行业务逻辑，只存储配置
        // 实际的路由注册在发布时完成
        Map<String, Object> config = node.getConfig();

        // 保存协议类型到上下文，供后续节点使用
        context.setVariable("_protocol", config.get("protocol"));
        context.setVariable("_apiConfig", config);

        // 透传输入
        return input;
    }

    @Override
    public String getType() {
        return "api-gateway";
    }
}
