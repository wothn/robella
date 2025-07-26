package org.elmo.robella.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        RedisSerializationContext<String, Object> serializationContext = 
            RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}