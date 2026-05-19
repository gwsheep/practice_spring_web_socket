package com.devgwon.practice.springwebsocket.util;

import com.devgwon.practice.springwebsocket.dto.ChatResponse;
import com.devgwon.practice.springwebsocket.domain.ChatUser;
import com.devgwon.practice.springwebsocket.enums.ChatType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

//단일 WAS에서는 괜찮으나, 다중 환경에서는 Redis 등 활용 필요
@Component
@RequiredArgsConstructor
public class ChatManager {

    private static final long LEAVE_DELAY_SECONDS = 3;

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    private final Map<String, ChatUser> chatUserMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingMap = new ConcurrentHashMap<>();

    public void enter(Long roomId, String userName, String sessionId) {

        String key = setKey(roomId, userName);
        ScheduledFuture<?> pending = pendingMap.remove(key);
        if(pending != null) {
            pending.cancel(false);
        }

        boolean isOnline = chatUserMap.containsKey(key);
        chatUserMap.put(key, new ChatUser(roomId, userName, sessionId));
        sessionMap.put(sessionId, key);

        if(!isOnline) {
            broadcast(roomId, userName, ChatType.ENTER, userName + "님이 입장하였습니다");
        }

        broadcastUserList(roomId);

    }

    public List<Long> getActiveRoomsIds() {
        return chatUserMap.values()
                            .stream()
                            .map(ChatUser::getRoomId)
                            .distinct()
                            .sorted()
                            .toList();
    }

    public void leave(Long roomId, String userName, String sessionId) {

        String key = setKey(roomId, userName);
        ScheduledFuture<?> pending = pendingMap.remove(key);
        if(pending != null) {
            pending.cancel(false);
        }

        chatUserMap.remove(key);
        sessionMap.remove(sessionId);

        broadcast(roomId, userName, ChatType.LEAVE, userName + "님이 퇴장하였습니다");
        broadcastUserList(roomId);

    }

    public void disconnect(String sessionId) {

        //session 정보나 user 정보가 없을 때
        String key = sessionMap.get(sessionId);
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

        broadcast(chatUser.getRoomId(), chatUser.getUserName(), ChatType.LEAVE, chatUser.getUserName() + "님이 퇴장하였습니다");
        broadcastUserList(chatUser.getRoomId());

    }

    private void broadcast(Long roomId, String userName, ChatType type, String message) {

        ChatResponse chat = ChatResponse.builder()
                                        .roomId(roomId)
                                        .userName(userName)
                                        .type(type)
                                        .message(message)
                                        .build();

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, chat);

    }

    private void broadcastUserList(Long roomId) {

        List<String> users = chatUserMap.values().stream()
                                        .filter(user -> roomId.equals(user.getRoomId()))
                                        .map(ChatUser::getUserName)
                                        .distinct()
                                        .toList();

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/users", users);


    }


    private String setKey(Long roomId, String userName) {
            return roomId + " : " + userName;
    }

}
