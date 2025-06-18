package com.sky.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.context.annotation.Primary;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RedisConfiguration {
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板对象");
        
        // 创建RedisTemplate对象，设置泛型为<String, String>
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        
        // 设置redis的链接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        
        // 创建String类型的序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // 设置各种类型的序列化器
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        
        // 调用afterPropertiesSet方法，初始化其他配置
        redisTemplate.afterPropertiesSet();
        
        return redisTemplate;
    }
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){
        log.info("开始创建处理对象的redis模板");
        
        // 创建RedisTemplate对象，设置泛型为<String, Object>
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        
        // 设置redis的链接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        
        // 创建String类型的序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // 创建使用自定义ObjectMapper的JSON序列化器
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 设置各种类型的序列化器
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);
        
        // 调用afterPropertiesSet方法，初始化其他配置
        redisTemplate.afterPropertiesSet();
        
        return redisTemplate;
    }
}