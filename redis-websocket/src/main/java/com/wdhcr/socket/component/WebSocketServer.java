package com.wdhcr.socket.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wdhcr.socket.constant.Constants;
import com.wdhcr.socket.utils.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint("/websocket/{id}")
@Component
public class WebSocketServer {

    private static final long sessionTimeout = 600000;

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 当前在线连接数
     */
    private static AtomicInteger onlineCount = new AtomicInteger(0);

    /**
     * 用来存放每个客户端对应的 WebSocketServer 对象
     */
    private static ConcurrentHashMap<Long, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收 id
     */
    private Long id;


    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("id") Long id) {
        session.setMaxIdleTimeout(sessionTimeout);
        this.session = session;
        this.id = id;
        if (webSocketMap.containsKey(id)) {
            webSocketMap.remove(id);
            webSocketMap.put(id, this);
        } else {
            webSocketMap.put(id, this);
            addOnlineCount();
        }
        log.info("编号id:" + id + "连接,当前在线数为:" + getOnlineCount());
        try {
            sendMessage("连接成功！");
        } catch (IOException e) {
            log.error("编号id:" + id + ",网络异常!!!!!!");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(id)) {
            webSocketMap.remove(id);
            subOnlineCount();
        }
        log.info("编号id:" + id + "退出,当前在线数为:" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject jsonObject = JSON.parseObject(message);
        String key = jsonObject.get(Constants.REDIS_MESSAGE_KEY).toString();
        String value = jsonObject.get(Constants.REDIS_MESSAGE_VALUE).toString();
        sendMessage(key,value);
        log.info("编号id消息:" + id + ",报文:" + message);
    }

    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("编号id错误:" + this.id + ",原因:" + error.getMessage());
        error.printStackTrace();
    }
    

    /**
     * @description:  分布式  使用redis 去发布消息
     * @dateTime: 2021/6/17 10:31
     */
    public void sendMessage(String key,String message) {
        String newMessge= null;
        try {
            newMessge = new String(message.getBytes(Constants.UTF8), Constants.UTF8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map<String,String> map = new HashMap<String, String>();
        map.put(Constants.REDIS_MESSAGE_KEY, key);
        map.put(Constants.REDIS_MESSAGE_VALUE, newMessge);

        /**
         *
         * spring管理的都是单例（singleton）和 websocket （多对象）相冲突。
         *
         * 需要了解一个事实：websocket 是多对象的，每个用户的聊天客户端对应 java 后台的一个 websocket 对象，前后台一对一（多对多）实时连接，
         * 所以 websocket 不可能像 servlet 一样做成单例的，让所有聊天用户连接到一个 websocket对象，这样无法保存所有用户的实时连接信息。
         * 可能 spring 开发者考虑到这个问题，没有让 spring 创建管理 websocket ，而是由 java 原来的机制管理websocket ，所以用户聊天时创建的
         * websocket 连接对象不是 spring 创建的，spring 也不会为不是他创建的对象进行依赖注入，所以如果不用static关键字，每个 websocket 对象的 service 都是 null。
         */

        StringRedisTemplate template = SpringUtils.getBean(StringRedisTemplate.class);
        template.convertAndSend(Constants.REDIS_CHANNEL, JSON.toJSONString(map));
    }

    /**
     * @description: 单机使用  外部接口通过指定的客户id向该客户推送消息。
     * @dateTime: 2021/6/16 17:49
     */
    public void sendMessageByWayBillId(Long key, String message) {
        WebSocketServer webSocketServer = webSocketMap.get(key);
        if (!StringUtils.isEmpty(webSocketServer)) {
            try {
                webSocketServer.sendMessage(message);
                log.info("编号id为："+key+"发送消息："+message);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("编号id为："+key+"发送消息失败");
            }
        }
        log.error("编号id号为："+key+"未连接");
    }
    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public static synchronized AtomicInteger getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount.getAndIncrement();
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount.getAndDecrement();
    }
}
