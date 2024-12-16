package com.hmdp;


import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.impl.ShopServiceImpl;

import com.hmdp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class testMQ {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Test
    public void testQueue(){
        String queuename="hello.queue1";
        String msg="Hello World! 2";
        rabbitTemplate.convertAndSend(queuename,msg);
    }
}
