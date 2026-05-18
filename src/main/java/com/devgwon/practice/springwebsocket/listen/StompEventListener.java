package com.devgwon.practice.springwebsocket.listen;

import com.devgwon.practice.springwebsocket.util.ChatManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final ChatManager chatManager;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event){
        chatManager.disconnect(event.getSessionId());
    }


}
