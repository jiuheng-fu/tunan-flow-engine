package com.tunan.flow.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.dto.FlowDefinitionDTO;
import com.tunan.flow.dto.FlowEdge;
import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.dto.InterfaceConfig;
import com.tunan.flow.engine.FlowExecutor;
import com.tunan.flow.engine.gateway.GatewayRegistrar;
import com.tunan.flow.entity.FlowDefinition;
import com.tunan.flow.mapper.FlowDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流程发布服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowPublishService {

    private final FlowDefinitionMapper flowMapper;
    private final FlowExecutor flowExecutor;
    private final GatewayRegistrar gatewayRegistrar;
    private final ObjectMapper objectMapper;

    /**
     * 发布流程
     */
    @Transactional
    public void publish(String flowId) {
        log.info("开始发布流程: {}", flowId);

        // 1. 获取流程定义
        FlowDefinition flowDef = flowMapper.selectById(flowId);
        if (flowDef == null) {
            throw new RuntimeException("流程不存在: " + flowId);
        }

        // 2. 转换为 DTO
        FlowDefinitionDTO dto = convertToDTO(flowDef);

        // 3. 验证流程配置
        validateFlow(dto);

        // 4. 发布到执行引擎（内存缓存）
        flowExecutor.publish(flowId, dto);

        // 5. 注册网关（动态路由）
        gatewayRegistrar.register(flowId, dto, flowExecutor);

        // 6. 更新流程状态
        flowDef.setStatus("published");
        flowDef.setPublishedAt(LocalDateTime.now());
        flowMapper.updateById(flowDef);

        log.info("流程发布成功: {}", flowId);
    }

    /**
     * 取消发布
     */
    @Transactional
    public void unpublish(String flowId) {
        log.info("取消发布流程: {}", flowId);

        // 1. 获取流程定义
        FlowDefinition flowDef = flowMapper.selectById(flowId);
        if (flowDef == null) {
            throw new RuntimeException("流程不存在: " + flowId);
        }

        // 2. 转换为 DTO
        FlowDefinitionDTO dto = convertToDTO(flowDef);

        // 3. 取消注册网关
        gatewayRegistrar.unregister(flowId, dto);

        // 4. 从执行引擎移除
        flowExecutor.unpublish(flowId);

        // 5. 更新流程状态
        flowDef.setStatus("draft");
        flowDef.setPublishedAt(null);
        flowMapper.updateById(flowDef);

        log.info("流程取消发布成功: {}", flowId);
    }

    /**
     * 重新发布（更新后自动重新发布）
     */
    @Transactional
    public void republish(String flowId) {
        log.info("重新发布流程: {}", flowId);
        unpublish(flowId);
        publish(flowId);
    }

    /**
     * 转换 FlowDefinition 为 DTO
     */
    private FlowDefinitionDTO convertToDTO(FlowDefinition flowDef) {
        FlowDefinitionDTO dto = new FlowDefinitionDTO();
        dto.setId(flowDef.getId());
        dto.setName(flowDef.getName());
        dto.setDescription(flowDef.getDescription());

        Map<String, Object> json = flowDef.getDefinitionJson();
        if (json == null) {
            log.warn("流程定义为空: {}", flowDef.getId());
            return dto;
        }

        // 转换 nodes
        Object nodesObj = json.get("nodes");
        if (nodesObj instanceof List) {
            List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) nodesObj;
            List<FlowNode> nodes = new ArrayList<>();
            for (Map<String, Object> nodeMap : nodeMaps) {
                FlowNode node = objectMapper.convertValue(nodeMap, FlowNode.class);
                nodes.add(node);
            }
            dto.setNodes(nodes);
        }

        // 转换 edges
        Object edgesObj = json.get("edges");
        if (edgesObj instanceof List) {
            List<Map<String, Object>> edgeMaps = (List<Map<String, Object>>) edgesObj;
            List<FlowEdge> edges = new ArrayList<>();
            for (Map<String, Object> edgeMap : edgeMaps) {
                FlowEdge edge = objectMapper.convertValue(edgeMap, FlowEdge.class);
                edges.add(edge);
            }
            dto.setEdges(edges);
        }

        // 转换 config
        Object configObj = json.get("config");
        if (configObj != null) {
            InterfaceConfig config = objectMapper.convertValue(configObj, InterfaceConfig.class);
            dto.setConfig(config);
        }

        log.debug("转换完成: nodes={}, edges={}",
                dto.getNodes() != null ? dto.getNodes().size() : 0,
                dto.getEdges() != null ? dto.getEdges().size() : 0);

        return dto;
    }

    /**
     * 验证流程配置
     */
    private void validateFlow(FlowDefinitionDTO dto) {
        if (dto.getNodes() == null || dto.getNodes().isEmpty()) {
            throw new RuntimeException("流程没有节点");
        }

        // 检查是否有网关节点
        boolean hasGateway = dto.getNodes().stream()
                .anyMatch(node -> isGatewayNode(node.getType()));

        if (!hasGateway) {
            throw new RuntimeException("流程必须包含网关节点（API网关/REST API/Stream API/WebSocket/MCP工具）");
        }

        // 检查是否有响应节点
        boolean hasResponse = dto.getNodes().stream()
                .anyMatch(node -> "response".equals(node.getType()));

        if (!hasResponse) {
            throw new RuntimeException("流程必须包含响应节点");
        }
    }

    /**
     * 判断是否为网关节点
     */
    private boolean isGatewayNode(String nodeType) {
        return "api-gateway".equals(nodeType) ||
                "rest-api".equals(nodeType) ||
                "stream-api".equals(nodeType) ||
                "websocket-api".equals(nodeType) ||
                "mcp-tool".equals(nodeType) ||
                "function-call".equals(nodeType) ||
                "agent-skill".equals(nodeType);
    }
}
