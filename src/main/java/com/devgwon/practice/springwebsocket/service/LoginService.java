package com.devgwon.practice.springwebsocket.service;

import com.devgwon.practice.springwebsocket.dto.LoginRequest;
import com.devgwon.practice.springwebsocket.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final JwtTokenProvider jwtTokenProvider;

    public String createToken(LoginRequest request) {
        //추후 필요하면 DB or Redis에 Hashing 처리하여 저장 (TTL 등)
        return jwtTokenProvider.createAccessToken(request.getUser());
    }

}
