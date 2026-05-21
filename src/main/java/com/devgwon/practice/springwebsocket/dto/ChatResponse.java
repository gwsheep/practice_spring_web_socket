package com.devgwon.practice.springwebsocket.dto;

import com.devgwon.practice.springwebsocket.enums.ChatType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long messageId;

    private Long roomId;

    private String userName;

    private String message;

    private ChatType type;

    private Long fileId;

    private String fileName;

    private Long fileSize;

    private String contentType;

    private String downloadUrl;

    private LocalDateTime createdAt;

}
