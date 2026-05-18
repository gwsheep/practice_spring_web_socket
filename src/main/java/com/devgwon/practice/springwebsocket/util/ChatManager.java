package com.devgwon.practice.springwebsocket.util;

import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.domain.ChatUser;
import com.devgwon.practice.springwebsocket.enums.ChatType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
public class ChatManager {

    private static final long LEAVE_DELAY_SECONDS = 3;

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    //채팅 유저 고유 정보
    private final Map<String, ChatUser> chatUserMap = new ConcurrentHashMap<>();
    //세션 정보 (채팅 접속중인지 확인)
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();
    //leave 지연 정보
    private final Map<String, ScheduledFuture<?>> pendingMap = new ConcurrentHashMap<>();

    //sender는 임시
    public void enter(Long roomId, String sender, String sessionId) {

        String key = setKey(roomId, sender);
        ScheduledFuture<?> pending = pendingMap.remove(key);
        if(pending != null) {
            pending.cancel(false);
        }

        boolean isOnline = chatUserMap.containsKey(key);
        chatUserMap.put(key, new ChatUser(roomId, sender, sessionId));
        sessionMap.put(sessionId, key);

        if(!isOnline) {
            broadcast(roomId, sender, ChatType.ENTER, sender + "님이 입장하였습니다");
        }

    }

    public void leave(Long roomId, String sender, String sessionId) {

        String key = setKey(roomId, sender);
        ScheduledFuture<?> pending = pendingMap.remove(key);
        if(pending != null) {
            pending.cancel(false);
        }

        chatUserMap.remove(key);
        sessionMap.remove(sessionId);

        broadcast(roomId, sender, ChatType.LEAVE, sender + "님이 퇴장하였습니다");


    }

    public void disconnect(String sessionId) {

        //session 정보나 user 정보가 없을 때
        String key = sessionMap.remove(sessionId);
        if(key == null) {
            return;
        }
        ChatUser chatUser = chatUserMap.get(key);
        if(chatUser == null) {
            return;
        }

        ScheduledFuture<?> schedule = taskScheduler.schedule(
                () -> handleDelayedLeave(key, sessionId, chatUser),
                Instant.now().plusSeconds(LEAVE_DELAY_SECONDS)
        );
        pendingMap.put(key, schedule);

    }

    public void handleDelayedLeave(String key, String sessionId, ChatUser chatUser) {

        ChatUser user = chatUserMap.get(key);
        if(user == null || !sessionId.equals(user.getSessionId())) {
            pendingMap.remove(key);
            return;
        }

        chatUserMap.remove(key);
        pendingMap.remove(key);

        broadcast(chatUser.getRoomId(), chatUser.getSender(), ChatType.LEAVE, chatUser.getSender() + "님이 퇴장하였습니다");

    }

    private void broadcast(Long roomId, String sender, ChatType type, String message) {

        ChatResponse chat = ChatResponse.builder()
                .roomId(roomId)
                .sender(sender)
                .type(type)
                .message(message)
                .build();

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, chat);

    }


    private String setKey(Long roomId, String sender) {
            return roomId + " : " + sender;
    }

}
