package com.tunan.flow.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunan.flow.dto.FlowDefinitionDTO;
import com.tunan.flow.dto.FlowEdge;
import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.dto.InterfaceConfig;
import com.tunan.flow.entity.FlowDefinition;
import com.tunan.flow.mapper.FlowDefinitionMapper;
import com.tunan.flow.service.FlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class FlowServiceImpl extends ServiceImpl<FlowDefinitionMapper, FlowDefinition> implements FlowService {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public FlowDefinition create(FlowDefinitionDTO flowDef) {
        FlowDefinition flowDefinition = new FlowDefinition();
        flowDefinition.setName(flowDef.getName());
        flowDefinition.setDescription(flowDef.getDescription());
        //JSONObject definitionJson = JSONUtil.createObj();
        Map<String, Object> definitionJson = new HashMap<>();
        //definitionJson.put("nodes", new ArrayList<>());
        flowDefinition.setDefinitionJson(definitionJson);
        flowDefinition.setStatus("draft");
        this.save(flowDefinition);
        return flowDefinition;
    }

    @Override
    public FlowDefinitionDTO loadFlowDefinition(String flowId) {
        FlowDefinition flowDefinition = this.getById(flowId);
        if (flowDefinition != null) {
            FlowDefinitionDTO flowDef = new FlowDefinitionDTO();
            flowDef.setId(flowDefinition.getId());
            flowDef.setName(flowDefinition.getName());
            flowDef.setDescription(flowDefinition.getDescription());
            if(flowDefinition.getDefinitionJson() != null){
                Map<String, Object> definitionJson = flowDefinition.getDefinitionJson();

                // 🔥 修复：正确转换 nodes
                Object nodesObj = definitionJson.get("nodes");
                if (nodesObj instanceof List) {
                    List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) nodesObj;
                    List<FlowNode> nodes = new ArrayList<>();
                    for (Map<String, Object> nodeMap : nodeMaps) {
                        FlowNode node = objectMapper.convertValue(nodeMap, FlowNode.class);
                        nodes.add(node);
                    }
                    flowDef.setNodes(nodes);
                } else {
                    flowDef.setNodes(new ArrayList<>());
                }

                // 🔥 修复：正确转换 edges
                Object edgesObj = definitionJson.get("edges");
                if (edgesObj instanceof List) {
                    List<Map<String, Object>> edgeMaps = (List<Map<String, Object>>) edgesObj;
                    List<FlowEdge> edges = new ArrayList<>();
                    for (Map<String, Object> edgeMap : edgeMaps) {
                        FlowEdge edge = objectMapper.convertValue(edgeMap, FlowEdge.class);
                        edges.add(edge);
                    }
                    flowDef.setEdges(edges);
                } else {
                    flowDef.setEdges(new ArrayList<>());
                }

                // 正确转换 config
                if (definitionJson.containsKey("config") && definitionJson.get("config") != null) {
                    Object configObj = definitionJson.get("config");

                    // 如果已经是 InterfaceConfig，直接使用
                    if (configObj instanceof InterfaceConfig) {
                        flowDef.setConfig((InterfaceConfig) configObj);
                    }
                    // 如果是 Map，转换为 InterfaceConfig
                    else if (configObj instanceof Map) {
                        InterfaceConfig interfaceConfig = objectMapper.convertValue(configObj, InterfaceConfig.class);
                        flowDef.setConfig(interfaceConfig);
                    }
                    // 如果是其他类型，创建新的
                    else {
                        flowDef.setConfig(new InterfaceConfig());
                    }
                } else {
                    flowDef.setConfig(new InterfaceConfig());
                }
            }
            return flowDef;
        }
        return null;
    }

    @Override
    public void updateStatus(String flowId, String published) {
        FlowDefinition flowDefinition = this.getById(flowId);
        flowDefinition.setStatus(published);
        this.updateById(flowDefinition);
    }

    @Override
    public List<FlowDefinition> loadFlow() {
        return this.list();
    }

    @Override
    public FlowDefinition updateFlow(String flowId, FlowDefinitionDTO flowDef) {
        FlowDefinition flowDefinition = this.getById(flowId);
        flowDefinition.setName(flowDef.getName());
        flowDefinition.setDescription(flowDef.getDescription());
        this.updateById(flowDefinition);
        return flowDefinition;
    }

    @Override
    public void deleteFlow(String flowId) {
        //真删除后续改成逻辑删除
        this.removeById(flowId);
    }

    @Override
    public FlowDefinition designFlow(String flowId,FlowDefinitionDTO flowDef) {
        log.info("流程ID: {}", flowId);
        log.info("设计流程定义: {}", flowDef);
        FlowDefinition flowDefinition = this.getById(flowId);
        if (flowDefinition != null) {
            Map<String, Object> definitionJson = new HashMap<>();
            definitionJson.put("nodes", flowDef.getNodes());
            definitionJson.put("edges", flowDef.getEdges());
            definitionJson.put("config", flowDef.getConfig());
            flowDefinition.setDefinitionJson(definitionJson);
            log.info("json: {}", definitionJson);

            this.updateById(flowDefinition);
        }
        else {
            log.info("流程ID: {} 不存在", flowId);
        }

        return null;
    }
}
