package com.devgwon.practice.springwebsocket.controller;

import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.NoticeRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.service.ChatService;
import com.devgwon.practice.springwebsocket.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/socket/admin")
public class NoticeController {


    private final NoticeService noticeService;

    @PostMapping("/alarm")
    public void notice(@RequestBody @Valid ChatRequest req,
                       @RequestHeader(value = "Authorization", required = false) String authorization) {

        if(authorization == null) {
            throw new IllegalArgumentException("authorization 이 없습니다");
        }
        if(!authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 토큰이 요청되었습니다");
        }

        noticeService.notice(req, authorization);

    }

}
