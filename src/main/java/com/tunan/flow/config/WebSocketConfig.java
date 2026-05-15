package com.tunan.flow.config;

import com.tunan.flow.engine.gateway.FlowWebSocketServerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig  implements WebSocketConfigurer {

    @Autowired
    private FlowWebSocketServerHandler flowWebSocketServerHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 端点
        // 实际路径会由动态注册决定，这里先注册一个通用路径
        registry.addHandler(flowWebSocketServerHandler, "/ws/**")
                .setAllowedOrigins("*");
    }
}
