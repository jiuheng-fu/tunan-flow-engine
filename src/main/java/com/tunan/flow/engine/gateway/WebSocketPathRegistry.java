package com.tunan.flow.engine.gateway;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 路径注册表
 */
public class WebSocketPathRegistry {

    private static final Map<String, String> pathToFlowId = new ConcurrentHashMap<>();

    public static void register(String path, String flowId) {
        pathToFlowId.put(path, flowId);
    }

    public static void unregister(String path) {
        pathToFlowId.remove(path);
    }

    public static String getFlowId(String path) {
        return pathToFlowId.get(path);
    }
}
