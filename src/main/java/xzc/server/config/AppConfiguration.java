package xzc.server.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import xzc.server.util.SnowflakeIdWorker;

@Configuration
public class AppConfiguration {

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker(
            @Value("${app.snowflake.worker-id:0}") int workerId,
            @Value("${app.snowflake.dataCenter-id:0}") int dataCenterId) {
        return new SnowflakeIdWorker(workerId, dataCenterId);
    }

    /**
     * 设置 redisTemplate 操作对象序列化方式
     *
     * @param redissonConnectionFactory
     * @return
     */
    @Primary
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redissonConnectionFactory);
        Jackson2JsonRedisSerializer<?> jackson2JsonRedisSerializer = buildJackson2JsonRedisSerializer();
        //设置键（key）的序列化方式
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //设置值（value）的序列化方式
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);

        // 设置 Hash 的 序列化方式
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redissonConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redissonConnectionFactory);
        return template;
    }

    private Jackson2JsonRedisSerializer buildJackson2JsonRedisSerializer() {
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

}
