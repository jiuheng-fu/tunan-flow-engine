package com.tunan.flow.engine.gateway;

import com.tunan.flow.dto.FlowDefinitionDTO;
import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.FlowExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 网关注册器 - 统一管理所有网关类型的注册
 */
@Slf4j
@Component
public class GatewayRegistrar {

    @Autowired
    private RestApiRegistrar restApiRegistrar;

    @Autowired
    private StreamApiRegistrar streamApiRegistrar;

    @Autowired
    private WebSocketRegistrar webSocketRegistrar;

    @Autowired
    private McpServer mcpServer;

    /**
     * 注册流程的网关入口
     */
    public void register(String flowId, FlowDefinitionDTO flowDef, FlowExecutor flowExecutor) {
        // 查找网关节点
        FlowNode gatewayNode = findGatewayNode(flowDef);
        if (gatewayNode == null) {
            log.warn("流程 {} 没有网关节点，跳过注册", flowId);
            return;
        }

        Map<String, Object> config = gatewayNode.getConfig();
        String protocol = (String) config.getOrDefault("protocol", "rest");
        String path = (String) config.get("path");

        log.info("注册网关: flowId={}, protocol={}, path={}", flowId, protocol, path);

        switch (protocol) {
            case "rest":
                String method = (String) config.getOrDefault("method", "POST");
                restApiRegistrar.register(flowId, path, method);
                break;

            case "stream":
                String streamType = (String) config.getOrDefault("streamType", "sse");
                Integer heartbeat = (Integer) config.getOrDefault("heartbeat", 5000);
                streamApiRegistrar.register(flowId, path, streamType, heartbeat, flowExecutor);
                break;

            case "websocket":
                String wsMode = (String) config.getOrDefault("wsMode", "server");
                String wsTargetUrl = (String) config.get("wsTargetUrl");
                webSocketRegistrar.register(flowId, path, wsMode, wsTargetUrl, flowExecutor);
                break;

            case "mcp":
                mcpServer.registerTool(flowId, config, flowExecutor);
                break;

            case "function":
                // OpenAI Function Calling
                registerFunctionCalling(flowId, config);
                break;

            case "skill":
                // Agent Skill
                registerAgentSkill(flowId, config);
                break;

            default:
                log.warn("不支持的协议类型: {}", protocol);
        }
    }

    /**
     * 取消注册
     */
    public void unregister(String flowId, FlowDefinitionDTO flowDef) {
        FlowNode gatewayNode = findGatewayNode(flowDef);
        if (gatewayNode == null) return;

        Map<String, Object> config = gatewayNode.getConfig();
        String protocol = (String) config.getOrDefault("protocol", "rest");
        String path = (String) config.get("path");

        switch (protocol) {
            case "rest":
                restApiRegistrar.unregister(path);
                break;
            case "stream":
                streamApiRegistrar.unregister(path);
                break;
            case "websocket":
                webSocketRegistrar.unregister(path);
                break;
            case "mcp":
                String toolName = (String) config.get("toolName");
                mcpServer.unregisterTool(toolName);
                break;
        }
    }

    /**
     * 查找网关节点
     */
    private FlowNode findGatewayNode(FlowDefinitionDTO flowDef) {
        if (flowDef.getNodes() == null) return null;

        return flowDef.getNodes().stream()
                .filter(node -> {
                    String type = node.getType();
                    return "api-gateway".equals(type) ||
                            "rest-api".equals(type) ||
                            "stream-api".equals(type) ||
                            "websocket-api".equals(type) ||
                            "mcp-tool".equals(type) ||
                            "function-call".equals(type) ||
                            "agent-skill".equals(type);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 注册 OpenAI Function Calling
     */
    private void registerFunctionCalling(String flowId, Map<String, Object> config) {
        String functionName = (String) config.get("functionName");
        String path = "/api/functions/" + functionName;

        // 复用 REST API 注册器
        restApiRegistrar.register(flowId, path, "POST");
        log.info("注册 Function Calling: {} -> {}", functionName, path);
    }

    /**
     * 注册 Agent Skill
     */
    private void registerAgentSkill(String flowId, Map<String, Object> config) {
        String skillName = (String) config.get("skillName");
        String path = "/api/skills/" + skillName;

        // 复用 REST API 注册器
        restApiRegistrar.register(flowId, path, "POST");
        log.info("注册 Agent Skill: {} -> {}", skillName, path);
    }
}
