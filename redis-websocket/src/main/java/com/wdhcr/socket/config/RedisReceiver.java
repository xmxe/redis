package com.wdhcr.socket.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wdhcr.socket.component.WebSocketServer;
import com.wdhcr.socket.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 消息监听对象，接收订阅消息
 */
@Component
public class RedisReceiver implements MessageListener {
    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WebSocketServer webSocketServer;


    /**
     * 处理接收到的订阅消息
     */
    @Override
    public void onMessage(Message message, byte[] pattern)
    {
        String channel = new String(message.getChannel());// 订阅的频道名称
        String msg = "";
        try
        {
            msg = new String(message.getBody(), Constants.UTF8);//注意与发布消息编码一致，否则会乱码
            if (!StringUtils.isEmpty(msg)){
                if (Constants.REDIS_CHANNEL.endsWith(channel))// 最新消息
                {
                    JSONObject jsonObject = JSON.parseObject(msg);
                    webSocketServer.sendMessageByWayBillId(
                            Long.parseLong(jsonObject.get(Constants.REDIS_MESSAGE_KEY).toString())
                            ,jsonObject.get(Constants.REDIS_MESSAGE_VALUE).toString());
                }else{
                    //TODO 其他订阅的消息处理
                }

            }else{
                log.info("消息内容为空，不处理。");
            }
        }
        catch (Exception e)
        {
            log.error("处理消息异常："+e.toString());
            e.printStackTrace();
        }
    }
}

