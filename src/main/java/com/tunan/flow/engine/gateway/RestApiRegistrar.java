package com.tunan.flow.engine.gateway;


import com.tunan.flow.engine.FlowExecutor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API 注册器 - 动态注册 REST 接口
 */
@Slf4j
@Component
public class RestApiRegistrar {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private FlowExecutor flowExecutor;


    private final Map<String, RequestMappingInfo> registeredPaths = new ConcurrentHashMap<>();

    /**
     * 注册 REST API
     */
    public void register(String flowId, String path, String method) {

        try {
            // 为每个流程创建独立的 Handler 实例（绑定 flowId）
            FlowRestApiHandler handler = new FlowRestApiHandler(flowId, flowExecutor);

            // 获取处理方法
            Method handleMethod = handler.getClass()
                    .getMethod("handle",
                            HttpServletRequest.class,
                            HttpServletResponse.class,
                            Map.class, Map.class, Map.class, Map.class);

            // 构建映射信息
            RequestMappingInfo mappingInfo = RequestMappingInfo
                    .paths(path)
                    .methods(RequestMethod.valueOf(method.toUpperCase()))
                    .produces("application/json;charset=UTF-8")
                    .consumes("application/json;charset=UTF-8")
                    .build();

            // 注册到 Spring MVC
            requestMappingHandlerMapping.registerMapping(mappingInfo, handler, handleMethod);
            registeredPaths.put(path, mappingInfo);

            log.info("✅ REST API 注册成功: {} {} -> flowId: {}", method, path, flowId);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 取消注册
     */
    public void unregister(String path) {
        RequestMappingInfo mappingInfo = registeredPaths.remove(path);
        if (mappingInfo != null) {
            requestMappingHandlerMapping.unregisterMapping(mappingInfo);
            log.info("REST API 取消注册: {}", path);
        }
    }

}
