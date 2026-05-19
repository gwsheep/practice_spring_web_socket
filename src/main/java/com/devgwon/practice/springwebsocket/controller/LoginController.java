package com.devgwon.practice.springwebsocket.controller;


import com.devgwon.practice.springwebsocket.dto.LoginRequest;
import com.devgwon.practice.springwebsocket.dto.LoginResponse;
import com.devgwon.practice.springwebsocket.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Jwt 활용한 로그인
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {

        String accessToken = loginService.createToken(request);
        LoginResponse response = LoginResponse.builder().accessToken(accessToken).tokenType("Bearer ").build();
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

}
