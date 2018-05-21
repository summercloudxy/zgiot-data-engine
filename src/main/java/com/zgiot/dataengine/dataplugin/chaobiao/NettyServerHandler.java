package com.zgiot.dataengine.dataplugin.chaobiao;

import com.zgiot.common.pojo.DataModel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

/**
 * Created by Administrator on 2018\4\27 0027.
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Queue<DataModel> queue;

    private static Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    private String synHead = "eb90eb90eb90";

    private String metricCode;

    private String tcPrefix = "CB_";

    public NettyServerHandler(Queue<DataModel> queue, String defaultMetricCode) {
        this.queue = queue;
        this.metricCode = defaultMetricCode;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
        try {
            ByteBuf content = packet.content();
            String prefix = "";
            //判断数据至少大于12才会进入
            if (content.readableBytes() > 12) {
                prefix = ByteBufUtil.hexDump(content, 0, 6);
                if (synHead.equals(prefix)) {
                    String contentStr = ByteBufUtil.hexDump(content);
                    Integer num = Integer.parseInt(ByteBufUtil.hexDump(content, 8, 1), 16);//获得有多少组数据
                    String sub = contentStr.substring(24, contentStr.length()); //数据字符串
                    if (sub.length() > 2) {
                        Integer scope = Integer.parseInt(sub.substring(0, 2), 16);
                        if (scope >= 160 && scope <= 223) {
                            setDataModelList(sub, num);
                        } else {
                            logger.error("信息数据范围错误. (sub=`{}`, num=`{}`)", sub, num);
                        }
                    } else {
                        logger.error("sub length should greater than 2. (sub=`{}`, num=`{}`)", sub, num);
                    }
                } else {
                    logger.debug("synHead not matched with '{}'. (prefix=`{}`)", synHead, prefix);
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected exception happened.", e);
        }

    }

    public void setDataModelList(String sub, Integer num) {
        List<String> listStr = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < sub.length(); i++) {
            if (start + 12 <= sub.length()) {
                String substring = sub.substring(start, start + 12);
                start = start + 12;
                listStr.add(substring);
            } else {
                break;
            }
        }
        if (start != sub.length() || num != listStr.size()) {
            logger.error("数据长度错误.(sub=`{}`, num=`{}`)", sub, num);
        } else {
            for (String dateStr : listStr) {
                DataModel dataModel = new DataModel();
                StringBuilder sb = new StringBuilder();
                sb.append(tcPrefix);
                String tcStr = dateStr.substring(0, 2);
                if (StringUtils.isNotBlank(tcStr)) {
                    sb.append(tcStr.toUpperCase());
                }
                dataModel.setThingCode(sb.toString());
                dataModel.setMetricCode(metricCode);
                dataModel.setDataTimeStamp(new Date(System.currentTimeMillis()));
                Long msg = Long.parseLong(dateStr.substring(2, 8), 16);
                dataModel.setValue(msg.toString());
                queue.add(dataModel);
            }
        }
    }

}
