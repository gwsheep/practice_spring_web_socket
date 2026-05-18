package com.devgwon.practice.springwebsocket.domain;

import com.devgwon.practice.springwebsocket.enums.ChatType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name="chat_message")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "sender")
    private String sender;

    @Column(name = "message")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type")
    private ChatType chatType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private ChatFile chatFile;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChatMessage(Long roomId, String sender, String message, ChatType chatType, ChatFile chatFile) {
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.chatType = chatType;
        this.chatFile = chatFile;
    }

}
