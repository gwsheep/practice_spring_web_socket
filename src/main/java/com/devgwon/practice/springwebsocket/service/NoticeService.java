package com.devgwon.practice.springwebsocket.service;

import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.dto.NoticeRequest;
import com.devgwon.practice.springwebsocket.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    public void notice(ChatRequest req, String authorization) {

        String token = authorization.substring(7);
        if(!jwtTokenProvider.validate(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }
        String userName = jwtTokenProvider.getUserName(token);
        chatService.sendNotice(req, userName);

    }



}
