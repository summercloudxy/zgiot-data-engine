package com.zgiot.dataengine.dataplugin.chaobiao;

import com.zgiot.common.pojo.DataModel;
import com.zgiot.dataengine.common.queue.QueueManager;
import com.zgiot.dataengine.dataplugin.DataPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChaoBiaoDataPlugin implements DataPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaoBiaoDataPlugin.class);

    NettyServer nettyServer;

    @Value("${dataengine.chaobiao.server-port}")
    private int port;
    @Value("${dataengine.chaobiao.default-metriccode}")
    private String defaultMetricCode;

    @Override
    public void init() throws Exception {
        this.nettyServer = new NettyServer(QueueManager.getQueueCollected(), defaultMetricCode);
        LOGGER.info("ChaoBiaoDataPlugin inited.");
    }

    @Override
    public void start() throws Exception {
        if (port<=0){
            LOGGER.error("Chaobiao port invalid. (port=`{}`) ", this.port);
            return ;
        }

        try {
            this.nettyServer.run(this.port);
        } catch (Exception e) {
            LOGGER.error("Chaobiao failed to start.", e);
            return ;
        }

        LOGGER.info("ChaoBiaoDataPlugin started. (port=`{}`)", this.port);
    }

    @Override
    public int sendCommands(List<DataModel> datalist, List<String> errors) throws Exception {
        return 0;
    }

}
