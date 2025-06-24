package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类，用于注册WebSocket服务器
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注册WebSocket服务器
     * 该Bean会自动注册使用@ServerEndpoint注解声明的WebSocket服务器
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
} 