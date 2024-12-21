package com.hmdp.ordercomsumer.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.stereotype.Component;
@Slf4j
@Component
public class simpleListener   {


    @RabbitListener(queues = "hello.queue1")
    public void persistOrder( String msg)  throws InterruptedException{
System.out.println(msg);

    }
}
