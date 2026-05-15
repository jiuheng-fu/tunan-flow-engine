package com.tunan.flow.dto;

import lombok.Data;

@Data
public class FlowEdge {

    private String id;
    private String source;
    private String sourcePort;
    private String target;
    private String targetPort;
    private String condition;  // 条件表达式
}
