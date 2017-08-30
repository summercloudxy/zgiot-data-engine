package com.zgiot.dataengine.upforwarder;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * A websocket server, waiting for client (such as 'app-server' ) registers.
 */
@Configuration
@EnableWebSocket
public class UpforwarderConfig implements WebSocketConfigurer {
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new UpforwarderHandler(), "/ws-dataengine")
                .addInterceptors(new HandshakeInterceptor());
    }
}

