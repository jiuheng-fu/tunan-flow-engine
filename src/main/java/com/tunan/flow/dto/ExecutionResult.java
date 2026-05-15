package com.tunan.flow.dto;

import lombok.Data;

@Data
public class ExecutionResult {

    private String executionId;
    private boolean success;
    private String error;
    private Object result;
    private long costTime;
}
