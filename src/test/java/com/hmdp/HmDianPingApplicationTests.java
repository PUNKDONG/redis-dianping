package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.impl.ShopServiceImpl;

import com.hmdp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;


import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private  ShopServiceImpl shopService;

    @Resource
    private UserServiceImpl userService;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Test
    public void excuteSave2(){
        List<User> users= userService.list();
        // 2. 打开文件，准备写入 token 供 JMeter 使用
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("tokens.txt"))) {
            for (User user : users) {
                // 3. 创建 UserDTO，并生成唯一 token
                UserDTO userDTO = new UserDTO();
                BeanUtils.copyProperties(user, userDTO);
                String token = UUID.randomUUID().toString().replace("-", "");

                // 4. 将 UserDTO 对象转换为 Map 并保存到 Redis
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("nickName", userDTO.getNickName());
                userMap.put("icon", userDTO.getIcon());
                userMap.put("id", userDTO.getId().toString());

                String redisKey = "login:token:" + token;
                HashOperations<String, Object, Object> opsForHash = stringRedisTemplate.opsForHash();
                opsForHash.putAll(redisKey, userMap);
                stringRedisTemplate.persist(redisKey);

                // 5. 将 token 写入文件
                writer.write(token);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Token generation complete. Tokens saved to tokens.txt for JMeter testing.");
    }

}


