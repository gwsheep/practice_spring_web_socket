package com.devgwon.practice.springwebsocket.dto;

import com.devgwon.practice.springwebsocket.enums.ChatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequest {

    @NotNull(message = "roomId는 필수입니다")

    private Long roomId;

    @NotBlank(message = "admin은 필수입니다")
    private String admin;

    private String message;

    private ChatType type;

    private Long fileId;

    //private String role;

}
