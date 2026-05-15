package com.devgwon.practice.springwebsocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BasicController {

    @GetMapping()
    public String home() {
        return "redirect:/chat.html";
    }


}
