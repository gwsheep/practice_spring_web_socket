package com.devgwon.practice.springwebsocket.controller;


import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.NoticeRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.service.ChatService;
import com.devgwon.practice.springwebsocket.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/socket/admin")
public class NoticeController {

    private final ChatService chatService;
    private final NoticeService noticeService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @PostMapping("/alarm")
    public void notice(@RequestBody ChatRequest req) {

        ChatResponse notice = chatService.createChat(req);
        //ChatResponse notice = noticeService.createNotice(req);
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + req.getRoomId(), notice);

    }

}
