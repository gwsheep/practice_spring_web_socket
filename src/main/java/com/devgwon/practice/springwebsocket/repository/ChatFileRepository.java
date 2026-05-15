package com.devgwon.practice.springwebsocket.repository;

import com.devgwon.practice.springwebsocket.domain.ChatFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {
}
