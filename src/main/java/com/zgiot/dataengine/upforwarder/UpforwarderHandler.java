package com.zgiot.dataengine.upforwarder;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.common.ThreadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class UpforwarderHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(UpforwarderHandler.class);

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
        ThreadManager.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    /* wait for queue values and send out */
                    DataModel data = QueueManager.getQueueCollected().poll();
                    try {
                        if (data == null) {
                            Thread.sleep(100);
                            continue;
                        }

                        // send message
                        if (!session.isOpen()) {
                            logger.warn("Websocket session '{}' is closed ", session);
                            break;
                        }

                        String json = JSON.toJSONString(data);
                        session.sendMessage(new TextMessage(json));

                    } catch (Exception e) {
                        logger.error("Error: ", e);
                    }

                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            session.close();
        } catch (IOException e) {
            logger.error("Cannot close session on afterConnectionClosed ");
        }
        logger.info("afterConnectionClosed of session '{}'" , session);
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
        logger.error("Close session '{}' on handleTransportError ", session);
    }
}

