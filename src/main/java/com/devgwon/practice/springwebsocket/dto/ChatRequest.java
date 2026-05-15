package com.devgwon.practice.springwebsocket.dto;

import com.devgwon.practice.springwebsocket.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class ChatRequest {

    @NonNull
    private Long roomId;

    private String sender;

    private String message;

    private ChatType type;

    private Long fileId;

}
