package com.lawfirm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client subscribes to /topic/... and /user/queue/...
        registry.enableSimpleBroker("/topic", "/user");
        // Client sends messages to /app/...
        registry.setApplicationDestinationPrefixes("/app");
        // Used for @SendToUser (private messages)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")          // WebSocket handshake URL
                .setAllowedOriginPatterns("*")
                .withSockJS();               // SockJS fallback for older browsers
    }
}