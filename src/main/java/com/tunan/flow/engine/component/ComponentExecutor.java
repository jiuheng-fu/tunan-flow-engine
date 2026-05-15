package com.tunan.flow.engine.component;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;

import java.util.Map;

public interface ComponentExecutor {

    /**
     * 执行组件
     * @param node 节点配置
     * @param input 输入参数
     * @param context 执行上下文
     * @return 执行结果
     */
    Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context);

    /**
     * 获取组件类型
     */
    String getType();

    // 默认方法，子类可覆盖
    default Map<String, Object> getDefinition(Map<String, Object> config) {
        return Map.of();
    }
}
