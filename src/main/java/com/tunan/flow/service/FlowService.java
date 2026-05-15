package com.tunan.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tunan.flow.dto.FlowDefinitionDTO;
import com.tunan.flow.entity.FlowDefinition;

import java.util.List;

public interface FlowService  extends IService<FlowDefinition> {


    FlowDefinition create(FlowDefinitionDTO flowDef);

    FlowDefinitionDTO loadFlowDefinition(String flowId);

    void updateStatus(String flowId, String published);

    List<FlowDefinition> loadFlow();

    FlowDefinition updateFlow(String flowId, FlowDefinitionDTO flowDef);

    void deleteFlow(String flowId);

    FlowDefinition designFlow(String flowId ,FlowDefinitionDTO flowDef);
}
