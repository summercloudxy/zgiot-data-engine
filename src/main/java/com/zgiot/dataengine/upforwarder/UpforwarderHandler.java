package com.zgiot.dataengine.upforwarder;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.common.ThreadManager;
import com.zgiot.dataengine.dataprocessor.DataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.websocket.Session;
import java.io.IOException;

/**
 * A websocket server, waiting for client (such as 'app-server' ) registers.
 */
@Component
public class UpforwarderHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpforwarderHandler.class);

    @Autowired
    UpforwarderDataListener upforwarderDataListener;

    public void setUpforwarderDataListener(UpforwarderDataListener upforwarderDataListener) {
        this.upforwarderDataListener = upforwarderDataListener;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        logger.info("来自客户端的消息:" + message);
        while (true) {
            session.sendMessage(new TextMessage("i got: "
                    + message.getPayload() + System.currentTimeMillis()));
            Thread.sleep(100);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connected : " + session);
        upforwarderDataListener.addSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            upforwarderDataListener.removeSession(session);
            session.close();
        } catch (IOException e) {
            logger.error("Cannot close session on afterConnectionClosed ");
        }
        logger.info("afterConnectionClosed of session '{}'" , session);
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        upforwarderDataListener.removeSession(session);
        session.close(CloseStatus.SERVER_ERROR);
        logger.error("Close session '{}' on handleTransportError ", session);
    }

}

