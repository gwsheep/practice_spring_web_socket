package com.devgwon.practice.springwebsocket.controller;

import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@MessageMapping("/socket")
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/user/chat")
    public void send(@Valid ChatRequest req, SimpMessageHeaderAccessor headerAccessor) {
        chatService.sendChat(req, headerAccessor.getSessionId());
    }

}
