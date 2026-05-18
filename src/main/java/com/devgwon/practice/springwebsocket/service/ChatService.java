package com.devgwon.practice.springwebsocket.service;

import com.devgwon.practice.springwebsocket.domain.ChatFile;
import com.devgwon.practice.springwebsocket.domain.ChatMessage;
import com.devgwon.practice.springwebsocket.dto.ChatRequest;
import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.enums.ChatType;
import com.devgwon.practice.springwebsocket.repository.ChatMessageRepository;
import com.devgwon.practice.springwebsocket.util.ChatManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final FileService fileService;
    private final ChatMessageRepository chatMessageRepository;

    private final ChatManager chatManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void sendChat(ChatRequest req, String sessionId) {

        //Chat Type
        ChatType chatType = (req.getType() == null) ? ChatType.TEXT : req.getType();

        //Chat Type : ENTER, LEAVE
        if(chatType == ChatType.ENTER) {
            chatManager.enter(req.getRoomId(), req.getSender(), sessionId);
            return;
        }
        if(chatType == ChatType.LEAVE) {
            chatManager.leave(req.getRoomId(), req.getSender(), sessionId);
            return;
        }

        //Create Chat
        createChat(req, chatType);

    }

    @Transactional
    public void sendChat(ChatRequest req) {

        //Chat Type
        ChatType chatType = (req.getType() == null) ? ChatType.TEXT : req.getType();

        //Create Chat
        createChat(req, chatType);

    }

    private void createChat(ChatRequest req, ChatType chatType) {

        //Chat File
        ChatFile chatFile = null;
        if(req.getFileId() != null) {
            chatFile = fileService.getChatFile(req.getFileId());
        }

        //Chat 저장
        ChatMessage chat = new ChatMessage(
                req.getRoomId(),
                req.getSender(),
                req.getMessage(),
                chatType,
                chatFile
        );
        ChatMessage savedChat = chatMessageRepository.save(chat);

        //parsing and send
        ChatResponse chatResponse = toResponse(savedChat, chatFile);
        messagingTemplate.convertAndSend("/topic/rooms/" + req.getRoomId(), chatResponse);

    }

    private ChatResponse toResponse(ChatMessage savedChat, ChatFile chatFile) {

        return ChatResponse.builder()
                .messageId(savedChat.getId())
                .roomId(savedChat.getRoomId())
                .sender(savedChat.getSender())
                .message(savedChat.getMessage())
                .type(savedChat.getChatType())
                .fileId((chatFile != null) ? chatFile.getId() : null)
                .fileName(chatFile != null ? chatFile.getOriginalFileName() : null)
                .fileSize(chatFile != null ? chatFile.getFileSize() : null)
                .contentType(chatFile != null ? chatFile.getContentType() : null)
                .downloadUrl(chatFile != null ? "/socket/file/download/" + chatFile.getId() : null)
                .createdAt(savedChat.getCreatedAt())
                .build();

    }

}
