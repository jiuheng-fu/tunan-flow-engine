package com.tunan.flow.engine;


import cn.hutool.core.util.StrUtil;
import com.tunan.flow.dto.ExecutionResult;
import com.tunan.flow.dto.FlowDefinitionDTO;
import com.tunan.flow.dto.FlowEdge;
import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.component.ComponentExecutor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程执行引擎
 */
@Slf4j
@Component
public class FlowExecutor {


    private Map<String, ComponentExecutor> executorMap = new ConcurrentHashMap<>();

    @Autowired
    private List<ComponentExecutor> executorList;  // 改用 List



    // 缓存已发布的流程
    private final Map<String, FlowDefinitionDTO> flowCache = new ConcurrentHashMap<>();


    // ✅ 添加脚本引擎（用于条件判断）
    private final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("javascript");

    /**
     * 发布流程
     */
    public void publish(String flowId, FlowDefinitionDTO flowDef) {
        flowCache.put(flowId, flowDef);
        log.info("流程发布成功: {}", flowId);
    }

    /**
     * 取消发布
     */
    public void unpublish(String flowId) {
        flowCache.remove(flowId);
        log.info("流程下架: {}", flowId);
    }

    @PostConstruct
    public void init() {
        for (ComponentExecutor executor : executorList) {
            executorMap.put(executor.getType(), executor);
            log.info("注册执行器: {} -> {}", executor.getType(), executor.getClass().getSimpleName());
        }
        log.info("已注册的执行器: {}", executorMap.keySet());
    }

    /**
     * 执行流程
     */
    public ExecutionResult execute(String flowId, Map<String, Object> input) {
        FlowDefinitionDTO flow = flowCache.get(flowId);
        if (flow == null) {
            throw new RuntimeException("流程不存在或未发布: " + flowId);
        }

        String executionId = UUID.randomUUID().toString();
        log.info("开始执行流程: {}, executionId: {}", flowId, executionId);

        long totalStart = System.currentTimeMillis();

        try {
            // 创建执行上下文
            ExecutionContext context = new ExecutionContext();
            context.setExecutionId(executionId);
            context.setFlowId(flowId);
            context.setVariables(new HashMap<>(input));

            // 找到开始节点（没有输入连接的节点）
            FlowNode startNode = findStartNode(flow);

            if (startNode == null) {
                throw new RuntimeException("流程没有起始节点");
            }

            // 执行节点
            Object result = executeNode(flow, startNode, context);

            ExecutionResult execResult = new ExecutionResult();
            execResult.setExecutionId(executionId);
            execResult.setSuccess(true);
            execResult.setResult(result);
            execResult.setCostTime(System.currentTimeMillis() - totalStart);

            log.info("流程执行成功: {}, 耗时: {}ms", flowId, execResult.getCostTime());
            return execResult;

        } catch (Exception e) {
            log.error("流程执行失败: {}", flowId, e);
            ExecutionResult result = new ExecutionResult();
            result.setExecutionId(executionId);
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setCostTime(System.currentTimeMillis() - totalStart);
            return result;
        }
    }

    /**
     * 递归执行节点
     */
    private Object executeNode(FlowDefinitionDTO flow, FlowNode node, ExecutionContext context) {
        long startTime = System.currentTimeMillis();
        context.setCurrentNode(node.getId());

        try {
            log.debug("执行节点: {}", node.getName());

            // 1. 获取节点执行器
            ComponentExecutor executor = executorMap.get(node.getType());
            if (executor == null) {
                throw new RuntimeException("不支持的组件类型: " + node.getType());
            }

            // 2. 准备输入参数（从上下文获取）
            Map<String, Object> nodeInput = prepareInput(flow, node, context);

            // 3. 执行节点
            Object result = executor.execute(node, nodeInput, context);

            // 4. 保存结果到上下文（关键！）
            context.setLastResult(result);  // 添加这行
            context.setVariable(node.getId() + "_output", result);

            // 4. 保存结果到上下文
            if (node.getOutputs() != null) {
                for (String output : node.getOutputs()) {
                    context.setVariable(output, result);
                }
            }

            // 5. 记录执行日志
            logExecution(node, startTime, true, null);

            // 6. 查找下一个节点
            List<FlowNode> nextNodes = findNextNodes(flow, node, context);
            if (nextNodes.isEmpty()) {
                return result;
            }

            // 7. 执行下一个节点
            if (nextNodes.size() == 1) {
                return executeNode(flow, nextNodes.get(0), context);
            } else {
                // 并行执行
                // ✅ 并行执行优化
                return executeParallel(flow, nextNodes, context);
            }

        } catch (Exception e) {
            log.error("节点执行失败: {}", node.getName(), e);
            logExecution(node, startTime, false, e.getMessage());
            throw e;
        }
    }

    // ✅ 并行执行
    private Object executeParallel(FlowDefinitionDTO flow, List<FlowNode> nodes, ExecutionContext context) {
        List<Object> results = new ArrayList<>();
        for (FlowNode node : nodes) {
            try {
                Object result = executeNode(flow, node, context);
                results.add(result);
            } catch (Exception e) {
                log.error("并行节点执行失败: {}", node.getName(), e);
                results.add(Map.of("error", e.getMessage(), "node", node.getName()));
            }
        }
        return results;
    }

    /**
     * 准备节点输入
     */
    private Map<String, Object> prepareInput(FlowDefinitionDTO flow, FlowNode node,
                                             ExecutionContext context) {
        Map<String, Object> input = new HashMap<>();

        // 找到所有连接到当前节点的边
        List<FlowEdge> incomingEdges = findIncomingEdges(flow, node);

        for (FlowEdge edge : incomingEdges) {
            // 获取源节点的输出
            Object value = context.getVariable(edge.getSource() + "_output");
            if (value != null) {
                input.put(edge.getSourcePort(), value);
            }
        }

        // 合并全局变量
        input.putAll(context.getVariables());

        return input;
    }

    private FlowNode findStartNode(FlowDefinitionDTO flow) {
        if (flow.getNodes() == null || flow.getNodes().isEmpty()) {
            return null;
        }

        if (flow.getEdges() == null || flow.getEdges().isEmpty()) {
            return flow.getNodes().get(0);
        }


        Set<String> hasInput = new HashSet<>();
        for (FlowEdge edge : flow.getEdges()) {
            hasInput.add(edge.getTarget());
        }

        for (FlowNode node : flow.getNodes()) {
            if (!hasInput.contains(node.getId())) {
                return node;
            }
        }
        return  flow.getNodes().get(0);
    }

    private List<FlowNode> findNextNodes(FlowDefinitionDTO flow, FlowNode node, ExecutionContext context) {
        List<FlowNode> nextNodes = new ArrayList<>();

        for (FlowEdge edge : flow.getEdges()) {
            if (edge.getSource().equals(node.getId())) {
                // ✅ 支持条件判断
                if (StrUtil.isNotBlank(edge.getCondition())) {
                    boolean matched = evaluateCondition(edge.getCondition(), context);
                    if (!matched) {
                        continue;
                    }
                }
                findNodeById(flow, edge.getTarget()).ifPresent(nextNodes::add);
            }
        }

        return nextNodes;
    }

    // ✅ 条件表达式求值
    private boolean evaluateCondition(String condition, ExecutionContext context) {
        try {
            scriptEngine.put("context", context);
            scriptEngine.put("input", context.getVariables());
            scriptEngine.put("output", context.getLastResult());
            scriptEngine.put("lastResult", context.getLastResult());

            Object result = scriptEngine.eval(condition);
            return Boolean.TRUE.equals(result);
        } catch (ScriptException e) {
            log.error("条件表达式执行失败: {}", condition, e);
            return false;
        }
    }

    private Optional<FlowNode> findNodeById(FlowDefinitionDTO flow, String nodeId) {
        return flow.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst();
    }

    private List<FlowEdge> findIncomingEdges(FlowDefinitionDTO flow, FlowNode node) {
        List<FlowEdge> edges = new ArrayList<>();
        for (FlowEdge edge : flow.getEdges()) {
            if (edge.getTarget().equals(node.getId())) {
                edges.add(edge);
            }
        }
        return edges;
    }

    private void logExecution(FlowNode node, long startTime, boolean success, String error) {
        // 这里可以实现日志记录 将来异步存储
        long duration = System.currentTimeMillis() - startTime;
        if (success) {
            log.debug("节点执行完成: {}, 耗时: {}ms", node.getName(), duration);
        } else {
            log.warn("节点执行失败: {}, 耗时: {}ms, 错误: {}", node.getName(), duration, error);
        }
    }
}
