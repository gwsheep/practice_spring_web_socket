package com.devgwon.practice.springwebsocket.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name="chat_file")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "stored_file_name")
    private String storedFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "storage_path")
    private String storagePath;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChatFile(String originalFileName, String storedFileName, Long fileSize, String contentType, String storagePath) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.storagePath = storagePath;
    }
}
