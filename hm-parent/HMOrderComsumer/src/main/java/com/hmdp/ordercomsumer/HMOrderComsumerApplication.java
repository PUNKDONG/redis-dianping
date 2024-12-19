package com.hmdp.ordercomsumer;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HMOrderComsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HMOrderComsumerApplication.class, args);
    }
    @Bean
    public MessageConverter jacksonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
