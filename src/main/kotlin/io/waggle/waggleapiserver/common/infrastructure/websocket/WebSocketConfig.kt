package io.waggle.waggleapiserver.common.infrastructure.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthHandshakeInterceptor: WebSocketAuthHandshakeInterceptor,
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 순수 WebSocket 엔드포인트
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://waggle.lol",
            ).addInterceptors(webSocketAuthHandshakeInterceptor)

        // SockJS 폴백 엔드포인트
        registry
            .addEndpoint("/ws-sockjs")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://waggle.lol",
            ).addInterceptors(webSocketAuthHandshakeInterceptor)
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.setApplicationDestinationPrefixes("/app")
        registry.enableSimpleBroker("/queue", "/topic")
        registry.setUserDestinationPrefix("/user")
    }
}
