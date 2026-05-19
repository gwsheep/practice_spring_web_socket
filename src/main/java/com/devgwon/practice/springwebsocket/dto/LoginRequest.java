package com.devgwon.practice.springwebsocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 로그인 (username으로 임시 구현)
 */
@Data
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "사용자 이름은 필수입니다")
    private String user;

}
