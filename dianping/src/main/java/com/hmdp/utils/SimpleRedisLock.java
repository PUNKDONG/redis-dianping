package com.hmdp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String lockName;
    private StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    public SimpleRedisLock(String lockName,StringRedisTemplate stringRedisTemplate) {
        this.lockName = lockName;
        this.stringRedisTemplate = stringRedisTemplate;
            }
    private String RANDOM_ID= UUID.randomUUID().toString().replace("-", "")+"-";
    @Override
    public boolean lock(long timeout) {
        String threadId = RANDOM_ID+Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockName, threadId, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    @Override
    public void unlock() {
    stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockName),RANDOM_ID+Thread.currentThread().getId());
    }
//    @Override
//    public void unlock() {
//        String LockthreadID = stringRedisTemplate.opsForValue().get(lockName);
//       String nowthreadID = RANDOM_ID+Thread.currentThread().getId();
//
//        if(nowthreadID.equals(LockthreadID)){
//            //可能在这个部分出现阻塞的问题，判断过了，但是一直没有执行删除，
//            //所以要用LUA脚本对着一个unlock函数进行原子化操作
//            stringRedisTemplate.delete(lockName);
//        }
//
//    }
}
