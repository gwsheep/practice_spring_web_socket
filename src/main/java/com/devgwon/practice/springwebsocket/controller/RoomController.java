package com.devgwon.practice.springwebsocket.controller;


import com.devgwon.practice.springwebsocket.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/socket/rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/list")
    public ResponseEntity<?> getRoomIds() {
        List<Long> roomIds = roomService.getRoomIds();
        return ResponseEntity.status(HttpStatus.OK).body(roomIds);
    }


}
