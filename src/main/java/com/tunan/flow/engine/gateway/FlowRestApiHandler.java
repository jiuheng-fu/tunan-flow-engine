package com.tunan.flow.engine.gateway;

import com.tunan.flow.dto.ExecutionResult;
import com.tunan.flow.engine.FlowExecutor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


/**
 * 统一动态API入口（推荐）
 */
@Slf4j
public class FlowRestApiHandler {

    private final String flowId;
    private final FlowExecutor flowExecutor;

    /**
     * 构造函数，每个流程注册时创建独立实例
     */
    public FlowRestApiHandler(String flowId, FlowExecutor flowExecutor) {
        this.flowId = flowId;
        this.flowExecutor = flowExecutor;
    }




    /**
     * 动态接口请求入口
     *
     * @param request       HttpServletRequest
     * @param response      HttpServletResponse
     * @param pathVariables 路径变量
     * @param defaultHeaders 请求头
     * @param parameters    表单参数和URL参数
     * @param body          json参数
     * @return 返回请求结果
     * @throws Throwable 处理失败抛出的异常
     */
    @ResponseBody
    public Object handle(HttpServletRequest request,
                         HttpServletResponse response,
                         @PathVariable(required = false) Map<String, Object> pathVariables,
                         @RequestHeader(required = false) Map<String, Object> defaultHeaders,
                         @RequestParam(required = false) Map<String, Object> parameters,
                         @RequestBody(required = false) Map<String, Object> body) throws Throwable {


        log.debug("动态API调用: flowId={}, method={}, path={}",
                flowId, request.getMethod(), request.getRequestURI());

        // 2. 合并所有参数
        Map<String, Object> input = new HashMap<>();
        if (pathVariables != null) input.putAll(pathVariables);
        if (parameters != null) input.putAll(parameters);
        if (body != null) input.putAll(body);

        // 3. 注入请求头（可选）
        input.put("_headers", defaultHeaders);
        input.put("_method", request.getMethod());
        input.put("_path", request.getRequestURI());

        // 执行流程
        ExecutionResult result = flowExecutor.execute(flowId, input);

        if (result.isSuccess()) {
            return Map.of(
                    "code", 200,
                    "success", true,
                    "data", result.getResult(),
                    "executionId", result.getExecutionId(),
                    "costTime", result.getCostTime()
            );
        } else {
            return Map.of("code", 500, "message", result.getError());
        }
    }
}
