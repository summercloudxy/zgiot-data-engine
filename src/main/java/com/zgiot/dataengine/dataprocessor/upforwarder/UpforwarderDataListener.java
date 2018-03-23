package com.zgiot.dataengine.dataprocessor.upforwarder;

import com.alibaba.fastjson.JSON;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataprocessor.DataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class UpforwarderDataListener implements DataListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpforwarderDataListener.class);
    private static AtomicLong errorCounter = new AtomicLong(0);
    private static final int WARN_PER_ITEM = 10000;

    private static List<WebSocketSession> sessions = new ArrayList<>();  // may multiple sessions in future

    public void addSession(WebSocketSession s) {
        synchronized (sessions) {
            // multiple sessions will cause client reconnection got multip connections for duplicated data.
            // not sure why, so only support single session to workaround as per now.
            if (sessions.size() == 0) {
                sessions.add(s);
            }
        }

        BlockingQueue<DataModel> buffer = (BlockingQueue) QueueManager.getPreWss();
        flushBuffer(buffer);
    }

    public void removeSession(WebSocketSession s) {
        synchronized (sessions) {
            sessions.remove(s);
        }
    }

    @Override
    public void onData(DataModel data) {
        if (data == null) {
            return;
        }

        BlockingQueue<DataModel> buffer = (BlockingQueue) QueueManager.getPreWss();
        try {
            buffer.add(data);
        } catch (Exception e) {
            long count = errorCounter.incrementAndGet();
            if ((count % WARN_PER_ITEM) == 1) {
                LOGGER.warn("Upforwarder buffer maybe full, buffer size is {}, error msg is `{}`.  ", buffer.size()
                        , e.getMessage());
            }
        }

        flushBuffer(buffer);

    }

    private void flushBuffer(BlockingQueue<DataModel> buffer) {
        if (sessions.size() == 0) {
            return;
        }

        final int RESERVED_SIZE = 1000;
        List<DataModel> list = new ArrayList(buffer.size() + RESERVED_SIZE);
        buffer.drainTo(list);
        // get from buffer and send them
        errorCounter.set(0);

        if (list.size() == 0) {
            return; // no data and cancel this flush
        }

        // do send to all sessions
        for (WebSocketSession session : sessions) {
            synchronized (session) {
                // send message
                if (!session.isOpen()) {
                    LOGGER.warn("Websocket session '{}' is closed ", session);
                    continue;
                }

                for (DataModel dm : list) {
                    String json = JSON.toJSONString(dm);
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        }
    }
}
