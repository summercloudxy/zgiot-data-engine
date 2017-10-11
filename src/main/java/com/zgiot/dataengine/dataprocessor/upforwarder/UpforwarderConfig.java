package com.zgiot.dataengine.dataprocessor.upforwarder;

import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    UpforwarderHandler upforwarderHandler;

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(upforwarderHandler, "/ws-dataengine")
                .addInterceptors(new HandshakeInterceptor());
    }
}

