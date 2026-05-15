package com.devgwon.practice.springwebsocket.controller;

import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/socket/user/chat")
    public void send(ChatRequest req) {

        if(req.getRoomId() == null) {
            throw new IllegalArgumentException("Room Id는 필수입니다");
        }
        ChatResponse chat = chatService.createChat(req);
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + req.getRoomId(), chat);
    }



}
