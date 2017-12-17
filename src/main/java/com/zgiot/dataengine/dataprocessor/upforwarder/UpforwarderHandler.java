package com.zgiot.dataengine.dataprocessor.upforwarder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * A websocket server, waiting for client (such as 'app-server' ) registers.
 */
@Component
public class UpforwarderHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpforwarderHandler.class);
    static final int BIT_WAIT = 100;

    @Autowired
    UpforwarderDataListener upforwarderDataListener;

    public void setUpforwarderDataListener(UpforwarderDataListener upforwarderDataListener) {
        this.upforwarderDataListener = upforwarderDataListener;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        LOGGER.info("来自客户端的消息:" + message);
        while (true) {
            session.sendMessage(new TextMessage("i got: "
                    + message.getPayload() + System.currentTimeMillis()));
            Thread.sleep(BIT_WAIT);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LOGGER.info("Connected : " + session);
        upforwarderDataListener.addSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            upforwarderDataListener.removeSession(session);
            session.close();
        } catch (IOException e) {
            LOGGER.error("Cannot close session on afterConnectionClosed ");
        }
        LOGGER.info("afterConnectionClosed of session '{}'" , session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        upforwarderDataListener.removeSession(session);
        session.close(CloseStatus.SERVER_ERROR);
        LOGGER.error("Close session '{}' on handleTransportError ", session);
    }

}
