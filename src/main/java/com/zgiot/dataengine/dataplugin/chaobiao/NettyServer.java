package com.zgiot.dataengine.dataplugin.chaobiao;


import com.zgiot.common.pojo.DataModel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

/**
 * Created by Administrator on 2018\4\27 0027.
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Queue<DataModel> queue;
    private String metricCode;


    public NettyServer(Queue<DataModel> queue, String defaultMetricCode) {
        this.queue = queue;
        this.metricCode = defaultMetricCode;
    }

    public void run(int port) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();

        //由于我们用的是UDP协议，所以要用NioDatagramChannel来创建
        b.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)//支持广播
                .handler(new NettyServerHandler(queue, this.metricCode));

        ChannelFuture sync = b.bind(port).sync();
        sync.addListener(future -> logger.info("neety启动成功"));

        sync.channel();//.closeFuture()

    }
}
