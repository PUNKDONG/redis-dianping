package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.3.8:6379").setPassword("123456");
        //config.useSingleServer().setAddress("redis://192.168.3.26:6379").setPassword("123456");
        return Redisson.create(config);
    }
}
