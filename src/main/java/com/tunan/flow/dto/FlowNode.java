package com.tunan.flow.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FlowNode {

    private String id;
    private String type;      // http, transform, filter, mcp,
    private String name;
    private Double x;         // 画布坐标
    private Double y;
    private Map<String, Object> config;  // 节点配置
    private List<String> inputs;         // 输入端口
    private List<String> outputs;        // 输出端口
}
