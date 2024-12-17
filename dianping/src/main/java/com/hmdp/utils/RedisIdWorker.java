package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static long nowStamp=1704067200L;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public long nextId(String keypre) {
        LocalDateTime now = LocalDateTime.now();
        long timeStamp=now.toEpochSecond(ZoneOffset.UTC)-nowStamp;
        //2.序列号
        String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr：" + keypre + ":" + yyyyMMdd);

        return timeStamp<<32 | increment;
    }

}
