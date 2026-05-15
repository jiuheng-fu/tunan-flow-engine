package com.tunan.flow.engine;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ExecutionContext {

    private String executionId;
    private String flowId;
    private String currentNode;
    private Map<String, Object> variables = new HashMap<>();
    private long startTime = System.currentTimeMillis();

    // 添加这个字段：存储上一个节点的执行结果
    private Object lastResult;

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    // ✅ 添加便捷方法
    public void setLastResult(Object result) {
        this.lastResult = result;
        variables.put("lastResult", result);
        variables.put("_lastResult", result);
    }

    public Object getLastResult() {
        return lastResult;
    }

    // 获取输入参数
    @SuppressWarnings("unchecked")
    public <T> T getInput(String key) {
        return (T) variables.get(key);
    }

    // 设置输出
    public void setOutput(String key, Object value) {
        variables.put(key, value);
    }

    public Object getOutput(String key) {
        return variables.get(key);
    }

    // 判断是否有变量
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }


}
