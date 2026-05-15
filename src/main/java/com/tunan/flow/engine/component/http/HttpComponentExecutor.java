package com.tunan.flow.engine.component.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.tunan.flow.dto.FlowNode;
import com.tunan.flow.engine.ExecutionContext;
import com.tunan.flow.engine.component.ComponentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class HttpComponentExecutor implements ComponentExecutor {


    private String replaceVariables(String str, Map<String, Object> variables) {
        if (str == null) return null;
        String result = str;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> replaceVariables(Map<String, Object> map, Map<String, Object> variables) {
        if (map == null) return null;
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                result.put(entry.getKey(), replaceVariables((String) value, variables));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    @Override
    public Object execute(FlowNode node, Map<String, Object> input, ExecutionContext context) {
        Map<String, Object> config = node.getConfig();

        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        // 🔥 修复：兼容处理 headers（可能是 String 或 Map）
        Map<String, Object> headers = parseHeaders(config.get("headers"));

        // 🔥 修复：兼容处理 body（可能是 String 或 Map）
        Map<String, Object> body = parseBody(config.get("body"));

        // 参数替换（支持表达式 {{variable}}）
        url = replaceVariables(url, input);
        headers = replaceVariables(headers, input);
        body = replaceVariables(body, input);

        log.debug("HTTP请求: {} {}", method, url);

        // 执行HTTP请求
        HttpRequest request = HttpRequest.of(url).method(Method.valueOf( method));

        if (headers != null) {
            headers.forEach((key, value) -> request.header(key, String.valueOf(value)));
        }

        if (body != null && ("POST".equals(method) || "PUT".equals(method))) {
            request.body(JSONUtil.toJsonStr(body));
        }

        HttpResponse response = request.execute();

        if (response.isOk()) {
            String responseBody = response.body();

            // 关键：将结果保存到上下文
            context.setLastResult(responseBody);

            // 尝试解析为 JSON
            try {
                return JSONUtil.parseObj(responseBody);
            } catch (Exception e) {
                return responseBody;
            }
        } else {
            throw new RuntimeException("HTTP请求失败: " + response.getStatus());
        }
    }

    @Override
    public String getType() {
        return "http";
    }

    /**
     * 解析 headers，兼容 String 和 Map 类型
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseHeaders(Object headersObj) {
        if (headersObj == null) {
            return null;
        }

        if (headersObj instanceof Map) {
            return (Map<String, Object>) headersObj;
        }

        if (headersObj instanceof String) {
            String headersStr = (String) headersObj;
            if (headersStr.trim().isEmpty()) {
                return null;
            }
            try {
                return JSONUtil.parseObj(headersStr);
            } catch (Exception e) {
                log.warn("解析 headers 失败: {}", headersStr);
                return null;
            }
        }

        return null;
    }

    /**
     * 解析 body，兼容 String 和 Map 类型
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(Object bodyObj) {
        if (bodyObj == null) {
            return null;
        }

        if (bodyObj instanceof Map) {
            return (Map<String, Object>) bodyObj;
        }

        if (bodyObj instanceof String) {
            String bodyStr = (String) bodyObj;
            if (bodyStr.trim().isEmpty()) {
                return null;
            }
            try {
                return JSONUtil.parseObj(bodyStr);
            } catch (Exception e) {
                // 如果不是 JSON 格式，作为纯文本处理
                Map<String, Object> result = new HashMap<>();
                result.put("text", bodyStr);
                return result;
            }
        }

        return null;
    }

}
