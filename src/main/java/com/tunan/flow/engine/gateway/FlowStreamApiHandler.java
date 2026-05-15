package com.tunan.flow.engine.gateway;

import com.tunan.flow.engine.FlowExecutor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;


/**
 * 流式 API 处理器
 * 每个流程实例独立
 */
@Slf4j
public class FlowStreamApiHandler {

    private final String flowId;
    private final FlowExecutor flowExecutor;
    private final StreamApiRegistrar registrar;


    public FlowStreamApiHandler(String flowId, FlowExecutor flowExecutor, StreamApiRegistrar registrar) {
        this.flowId = flowId;
        this.flowExecutor = flowExecutor;
        this.registrar = registrar;
    }

    /**
     * 处理 SSE 请求
     */
    public SseEmitter handle(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();
        log.info("SSE 连接建立: flowId={}, sessionId={}", flowId, sessionId);

        // 创建 SSE 连接
        SseEmitter emitter = registrar.createEmitter(flowId, sessionId);

        // 解析参数
        Map<String, String[]> paramMap = request.getParameterMap();
        Map<String, Object> params = new java.util.HashMap<>();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }

        // 异步执行流程
        new Thread(() -> {
            try {
                log.debug("开始执行流式流程: flowId={}", flowId);

                // 执行流程（这里需要改造 FlowExecutor 支持流式输出）
                Object result = flowExecutor.execute(flowId, params).getResult();

                // 推送结果
                if (result instanceof String) {
                    // 分块推送（模拟流式输出）
                    String text = (String) result;
                    for (int i = 0; i < text.length(); i++) {
                        registrar.push(sessionId, String.valueOf(text.charAt(i)));
                        Thread.sleep(50); // 模拟流式延迟
                    }
                } else {
                    registrar.push(sessionId, String.valueOf(result));
                }

                // 完成
                registrar.complete(sessionId);
                log.debug("流式流程执行完成: flowId={}", flowId);

            } catch (Exception e) {
                log.error("流式流程执行失败: flowId={}", flowId, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }).start();

        return emitter;
    }
}
