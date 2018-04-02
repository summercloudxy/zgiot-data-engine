package com.zgiot.dataengine.dataprocessor.rocketmqupforwarder;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.producer.MQProducer;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.remoting.common.RemotingHelper;
import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.dataprocessor.DataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocketMqUpforwarderDataListener implements DataListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMqUpforwarderDataListener.class);

    @Value("${dataengine.rocketmq.upforwarder-topic}")
    private String rktMqTopic;

    @Autowired
    MQProducer producer;

    @Override
    public void onData(DataModel data) {
        // send by rktmq
        try {
            String dataStr = JSON.toJSONString(data);

            Message msg = new Message(this.rktMqTopic,
                    null,
                    null,
                    dataStr.getBytes(RemotingHelper.DEFAULT_CHARSET));
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    LOGGER.debug("Msg sent ok, msg=`{}`, sendResult=`{}`", msg, sendResult);
                }

                @Override
                public void onException(Throwable e) {
                    LOGGER.error("Failed to send ", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to send msg. ", e);
        }
    }

}
