package com.tunan.flow.engine.component.gateway;

import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式API执行器
 * 支持 SSE (Server-Sent Events) 和 Chunked Transfer
 */

@Slf4j
@Component
public class StreamApiExecutor implements ComponentExecutor {
    // 存储流式会话
    private final Map<String, Sinks.Many<String>> streams = new ConcurrentHashMap<>();

    @Override
    public String getType() {
        return "stream-api";
    }

    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        context.setVariable("_gatewayConfig", config);
        context.setVariable("_protocol", "stream");
        context.setVariable("_streamType", config.getOrDefault("streamType", "sse"));

        log.debug("流式API执行器: type={}, path={}",
                config.get("streamType"), config.get("path"));

        return input;
    }

    /**
     * 推送流式数据
     */
    public void push(String sessionId, String chunk) {
        Sinks.Many<String> sink = streams.get(sessionId);
        if (sink != null) {
            sink.tryEmitNext(chunk);
        }
    }

    /**
     * 结束流式会话
     */
    public void complete(String sessionId) {
        Sinks.Many<String> sink = streams.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
