package com.devgwon.practice.springwebsocket.redis;

import com.devgwon.practice.springwebsocket.config.RedisConfig;
import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final ObjectMapper mapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void publish(ChatResponse req) {
        try {
            String request = mapper.writeValueAsString(req);
            stringRedisTemplate.convertAndSend(RedisConfig.CHAT_CHANNEL, request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

}
