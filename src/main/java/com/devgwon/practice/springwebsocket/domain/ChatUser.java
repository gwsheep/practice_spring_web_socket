package com.devgwon.practice.springwebsocket.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatUser {

    private Long roomId;
    private String userName;
    private String sessionId;

}
