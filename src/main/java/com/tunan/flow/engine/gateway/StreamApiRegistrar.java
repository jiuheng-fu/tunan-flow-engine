package com.tunan.flow.engine.gateway;

import com.tunan.flow.engine.FlowExecutor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 流式 API 注册器 - 支持 SSE (Server-Sent Events)
 */
@Slf4j
@Component
public class StreamApiRegistrar {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private FlowExecutor flowExecutor;

    // 存储已注册的路径
    private final Map<String, RequestMappingInfo> registeredPaths = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 心跳检测，保持连接
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            emitters.forEach((sessionId, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (IOException e) {
                    emitters.remove(sessionId);
                    log.debug("SSE 连接已断开: {}", sessionId);
                }
            });
        }, 30, 30, TimeUnit.SECONDS);
        log.info("StreamApiRegistrar 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        // 关闭所有连接
        emitters.forEach((id, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // ignore
            }
        });
        emitters.clear();
        heartbeatExecutor.shutdown();
        log.info("StreamApiRegistrar 销毁完成");
    }

    /**
     * 注册流式 API
     */
    public void register(String flowId, String path, String streamType,
                         Integer heartbeat, FlowExecutor flowExecutor) {
        try {
            // 创建处理器（每个流程独立实例）
            FlowStreamApiHandler handler = new FlowStreamApiHandler(flowId, flowExecutor, this);

            // 获取处理方法
            Method handleMethod = handler.getClass()
                    .getMethod("handle", HttpServletRequest.class, HttpServletResponse.class);

            // 构建映射信息
            RequestMappingInfo mappingInfo = RequestMappingInfo
                    .paths(path)
                    .methods(RequestMethod.GET)  // SSE 通常用 GET
                    .produces("text/event-stream;charset=UTF-8")
                    .build();

            // 注册到 Spring MVC
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handleMethod);
            registeredPaths.put(path, mappingInfo);

            log.info("✅ 流式 API 注册成功: GET {} -> flowId: {}", path, flowId);

        } catch (NoSuchMethodException e) {
            log.error("流式 API 注册失败: {}", path, e);
            throw new RuntimeException("流式 API 注册失败", e);
        }
    }

    /**
     * 创建 SSE 连接
     */
    public SseEmitter createEmitter(String flowId, String sessionId) {
        SseEmitter emitter = new SseEmitter(0L); // 永不超时

        emitter.onCompletion(() -> {
            emitters.remove(sessionId);
            log.debug("SSE 连接完成: {}", sessionId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(sessionId);
            log.debug("SSE 连接超时: {}", sessionId);
        });
        emitter.onError(e -> {
            emitters.remove(sessionId);
            log.debug("SSE 连接错误: {}", sessionId);
        });

        emitters.put(sessionId, emitter);

        // 发送初始连接成功事件
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException e) {
            log.error("发送连接事件失败", e);
        }

        return emitter;
    }

    /**
     * 推送消息
     */
    public void push(String sessionId, String data) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("message").data(data));
            } catch (IOException e) {
                emitters.remove(sessionId);
                log.debug("推送消息失败，连接已断开: {}", sessionId);
            }
        }
    }

    /**
     * 推送完成事件
     */
    public void complete(String sessionId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("complete").data("done"));
                emitter.complete();
            } catch (IOException e) {
                // ignore
            }
            emitters.remove(sessionId);
        }
    }

    /**
     * 取消注册
     */
    public void unregister(String path) {
        RequestMappingInfo mappingInfo = registeredPaths.remove(path);
        if (mappingInfo != null) {
            requestMappingHandlerMapping.unregisterMapping(mappingInfo);
            log.info("流式 API 取消注册: {}", path);
        }

        // 关闭该路径下的所有连接
        emitters.clear();
    }

}
