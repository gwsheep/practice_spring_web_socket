package com.devgwon.practice.springwebsocket.interceptor;

import com.devgwon.practice.springwebsocket.domain.ChatPrincipal;
import com.devgwon.practice.springwebsocket.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class JwtStompInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if(accessor == null) return message;

        if(StompCommand.CONNECT.equals(accessor.getCommand())){
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if(authorization == null || !authorization.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Authorization header 가 없습니다");
            }
            String webToken = authorization.substring(7);
            if(!jwtTokenProvider.validate(webToken)){
                throw new IllegalArgumentException("유효하지 않은 토큰입니다");
            }
            String userName = jwtTokenProvider.getUserName(webToken);
            accessor.setUser(new ChatPrincipal(userName));
        }

        return message;
    }


}
