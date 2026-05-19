package com.devgwon.practice.springwebsocket.service;

import com.devgwon.practice.springwebsocket.util.ChatManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ChatManager chatManager;

    public List<Long> getRoomIds() {
        return chatManager.getActiveRoomsIds();
    }

}
