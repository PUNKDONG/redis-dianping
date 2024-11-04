package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Slf4j
@Component
public class CacheClient
{
@Resource
private     StringRedisTemplate    stringRedisTemplate;
    @Autowired
    private HttpRequestHandlerAdapter httpRequestHandlerAdapter;

    public void  SetObjectToJsonWithTTL (Object obj, String Key , Long TTL, TimeUnit timeUnit){
    String jsonStr = JSONUtil.toJsonStr(obj);
    stringRedisTemplate.opsForValue().set(Key , jsonStr,TTL,timeUnit);
}
public void  SetObjectToJsonWithExpiretime  (Object obj, String Key , Long expiretime, TimeUnit timeUnit ){
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expiretime)));
        redisData.setData(obj);
        stringRedisTemplate.opsForValue().set(Key ,JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryByidWithNUll
        (String keypre, Class<R> type, ID id, Function<ID,R> dbSelect,Long RefreshTTL, TimeUnit timeUnit) {
        //首先在redis里面查询缓存,这里解决一个缓存穿透问题
        String cache  = stringRedisTemplate.opsForValue().get(keypre + id);
        //查询到了直接返回,以下判断首先判断是否为空，然后判断是否存在空字符串
        //如果查
        if(StrUtil.isNotBlank(cache)){
            R  bean = JSONUtil.toBean(cache , type);
            return bean;
        }
        if(cache !=null){
            //这里不为null就是空字符
            return null;
        }
        //没查询到就从数据库里面查，然后放到redis里面
        R dbBean =dbSelect.apply(id);
        if(dbBean == null){
            stringRedisTemplate.opsForValue().set(keypre + id, "",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);

            return null;
        }
        this.SetObjectToJsonWithTTL(dbBean,keypre+id,RefreshTTL, timeUnit);
        // 数据库也不存在就返回异常
        return  dbBean;
    }


    private static final ExecutorService CACHE_REBUILD_POLL= Executors.newFixedThreadPool(10);

    public<R,ID> R queryByidWithLogicTimeOut(ID id,Class<R> type,Function<ID,R> dbSelect,String keypre,Long RefreshTTL, TimeUnit timeUnit)   {
        LocalDateTime now = LocalDateTime.now();
        String cacheShop = stringRedisTemplate.opsForValue().get(keypre + id);
        if(StrUtil.isBlank(cacheShop)){
            return null;
        }

        RedisData redisData = JSONUtil.toBean(cacheShop, RedisData.class);
        R bean = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        if (redisData.getExpireTime().isAfter(now)){
            return bean;
        }

        //如果发现过期了，那么就首先获取锁，然后新开一个线程
        String lockkey=RedisConstants.LOCK_SHOP_KEY + id;
        if(trylock(lockkey)){
            //获取独立线程，重建
            CACHE_REBUILD_POLL.submit(()->{
                try {

                    R obj = dbSelect.apply(id);
                    this.SetObjectToJsonWithExpiretime(obj , keypre+id, RefreshTTL, timeUnit );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockkey);
                }
            });
        }

        cacheShop = stringRedisTemplate.opsForValue().get(keypre + id);
        redisData = JSONUtil.toBean(cacheShop, RedisData.class);
        bean = JSONUtil.toBean((JSONObject) redisData.getData(), type);

        return bean;

    }

    private boolean trylock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
