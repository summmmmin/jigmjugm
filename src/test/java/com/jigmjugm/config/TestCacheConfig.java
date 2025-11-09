package com.jigmjugm.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    public RedisCacheManager testCacheManager(RedisConnectionFactory cf) {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.findAndRegisterModules();

        var ser = RedisSerializationContext
                .SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer(om));

        var config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(ser)
                .entryTtl(Duration.ofSeconds(2)) // 2초
                .disableCachingNullValues();

        return RedisCacheManager.builder(cf)
                .cacheDefaults(config)
                .build();
    }
}
