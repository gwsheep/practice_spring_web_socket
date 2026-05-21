package com.devgwon.practice.springwebsocket.redis;

import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;


@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, @Nullable byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatResponse messages = mapper.readValue(body, ChatResponse.class);
            messagingTemplate.convertAndSend("/topic/rooms/" + messages.getRoomId(), messages);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
