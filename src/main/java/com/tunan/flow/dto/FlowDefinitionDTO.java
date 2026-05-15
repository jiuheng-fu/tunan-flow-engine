package com.tunan.flow.dto;

import lombok.Data;

import java.util.List;

@Data
public class FlowDefinitionDTO {

    private String id;
    private String name;
    private String description;
    private List<FlowNode> nodes;
    private List<FlowEdge> edges;
    private InterfaceConfig config;
}
