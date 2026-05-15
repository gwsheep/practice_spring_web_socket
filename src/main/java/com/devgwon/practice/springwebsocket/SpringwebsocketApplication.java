package com.devgwon.practice.springwebsocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SpringwebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringwebsocketApplication.class, args);
	}

}
