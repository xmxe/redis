package com.xmxe.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@EnableCaching//开启缓存
public class RedisConfig extends CachingConfigurerSupport{

    @Bean
    public KeyGenerator keyGenerator() {
     /*   return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(method.getName());
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };*/
    	return (target,method,params)->{
    		StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(method.getName());
            for (Object obj : params) {
                sb.append(obj.toString());
            }
            return sb.toString();
    	};
    }

    //通过自定义序列化，解决写入redis后乱码的问题
    @Bean(name="redisTemplate")
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        template.setConnectionFactory(factory);
        //key序列化方式
        template.setKeySerializer(redisSerializer);
        //value序列化
        template.setValueSerializer(redisSerializer);
        //value hashmap序列化
        template.setHashValueSerializer(redisSerializer);
        //key haspmap序列化
        template.setHashKeySerializer(redisSerializer);
        //
        return template;
    }

    /**
     * redis消息监听器容器
     * 可以添加多个监听不同话题的redis监听器，只需要把消息监听器和相应的消息订阅处理器绑定，该消息监听器
     * 通过反射技术调用消息订阅处理器的相关方法进行一些业务处理
     * @param connectionFactory 连接工厂
     *
     *
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅多个频道
        container.addMessageListener(messageListener(),new PatternTopic("topic"));
//        container.addMessageListener(messageListener2(),new ChannelTopic("analysis"));
//        container.addMessageListener(messageListener3,new PatternTopic("fullDataUpload"));

        //序列化对象（特别注意：发布的时候需要设置序列化；订阅方也需要设置序列化）
        Jackson2JsonRedisSerializer seria = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        seria.setObjectMapper(objectMapper);

        container.setTopicSerializer(seria);
        return container;
    }

    @Bean
    MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(new MessageListener(){
            @Override
            public void onMessage(Message message, byte[] pattern) {
                System.out.println("接收数据:"+message.toString());
                System.out.println("订阅频道:"+new String(message.getChannel()));
            }
        });
    }

}
