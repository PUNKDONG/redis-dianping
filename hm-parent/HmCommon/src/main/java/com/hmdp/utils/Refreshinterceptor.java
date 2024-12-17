package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class Refreshinterceptor implements HandlerInterceptor {
    private StringRedisTemplate redisTemplate;
    public Refreshinterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //TODO 从token获取，并刷新
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            return true;
        }
        //没有就退出
        Map<Object, Object> usermap = redisTemplate.opsForHash().entries("login:token:" + token);
        //有就刷新
        if(usermap.isEmpty()){
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);
        //TODO 获取的是map数据，map转换为user数据
        UserHolder.saveUser(userDTO);
//        redisTemplate.expire("login:token" + token,3000, TimeUnit.HOURS);
        redisTemplate.persist("login:token:" + token);

        //保存到threadlocal线程里面
        return true;

    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    UserHolder.removeUser();

    }
}
