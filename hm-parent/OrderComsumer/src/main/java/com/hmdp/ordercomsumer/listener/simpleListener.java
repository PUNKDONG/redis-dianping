package com.hmdp.ordercomsumer.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class simpleListener {

    @RabbitListener(queues = "work.queue")
    public void listen(String msg) throws InterruptedException{
        System.out.println("消费者1收到了"+msg);
        Thread.sleep(1000/5);
    }

    @RabbitListener(queues = "work.queue")
    public void listen2(String msg)throws InterruptedException{
        System.out.println("消费者2收到了"+msg);
        Thread.sleep(1000/50);
    }
}
