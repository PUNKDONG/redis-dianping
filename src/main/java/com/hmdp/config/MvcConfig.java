package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.Refreshnterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new Refreshnterceptor(stringRedisTemplate)).addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .excludePathPatterns(
                        "/shop/**",
                        "/shop-type/**",
                        "voucher/**",
                        "/blog/hot",
                        "/user/login",
                        "/user/code").order(1);
    }

}
