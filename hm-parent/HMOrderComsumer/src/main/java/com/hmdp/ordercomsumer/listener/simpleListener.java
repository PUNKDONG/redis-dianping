package com.hmdp.ordercomsumer.listener;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service. ISeckillVoucherService;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.hmdp.service.impl.VoucherServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class simpleListener   {


    @RabbitListener(queues = "hello.queue")
    public void persistOrder( String msg)  throws InterruptedException{


    }
}
