package com.tunan.flow.dto;

import com.tunan.flow.common.Result;
import lombok.Data;

import java.util.List;

@Data
public class InterfaceConfig {

    private String path;           // REST API路径
    private String method;         // GET/POST
    private String description;    // 接口描述
    private List<ApiParam> params; // 输入参数
    private Result<?> response;  // 输出定义
}
