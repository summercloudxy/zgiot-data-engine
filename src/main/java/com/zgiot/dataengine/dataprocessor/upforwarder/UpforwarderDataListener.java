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
    private static final Logger logger = LoggerFactory.getLogger(UpforwarderDataListener.class);
    private static AtomicLong errorCounter = new AtomicLong(0);
    private static final int WARN_PER_ITEM = 10000;

    private static List<WebSocketSession> sessions = new ArrayList<>();  // may multiple sessions in future

    public void addSession(WebSocketSession s) {
        sessions.add(s);
        BlockingQueue<DataModel> buffer = (BlockingQueue) QueueManager.getPreWss();
        flushBuffer(buffer);
    }

    public void removeSession(WebSocketSession s) {
        sessions.remove(s);
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
                logger.warn("Upforwarder buffer maybe full, buffer size is {}, error msg is `{}`.  ", buffer.size()
                        , e.getMessage());
            }
        }

        flushBuffer(buffer);

    }

    private void flushBuffer(BlockingQueue<DataModel> buffer) {
        for (WebSocketSession session : sessions) {
            synchronized (session) {
                // send message
                if (!session.isOpen()) {
                    logger.warn("Websocket session '{}' is closed ", session);
                    continue;
                }

                // get from buffer and send them
                List<DataModel> list = new ArrayList(buffer.size()+1000);
                buffer.drainTo(list);
                errorCounter.set(0);

                if (list.size() == 0) {
                    break; // cancel this flush
                }

                for (DataModel dm : list) {
                    String json = JSON.toJSONString(dm);
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }
    }
}
