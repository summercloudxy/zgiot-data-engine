package com.zgiot.dataengine.upforwarder;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.dataprocessor.DataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpforwarderDataListener implements DataListener {
    private static final Logger logger = LoggerFactory.getLogger(UpforwarderDataListener.class);

    private static List<WebSocketSession> sessions = new ArrayList<>();  // may multiple sessions in future

    public void addSession(WebSocketSession s) {
        sessions.add(s);
    }

    public void removeSession(WebSocketSession s) {
        sessions.remove(s);
    }

    @Override
    public void onData(DataModel data) {
        if (data == null) {
            return;
        }

        for (WebSocketSession session : sessions) {
            synchronized (session) {
                // send message
                if (!session.isOpen()) {
                    logger.warn("Websocket session '{}' is closed ", session);
                    return;
                }

                String json = JSON.toJSONString(data);
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
        }

    }
}
