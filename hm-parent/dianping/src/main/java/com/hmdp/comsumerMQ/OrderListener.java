package com.hmdp.comsumerMQ;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class OrderListener {

    @Autowired
    IVoucherOrderService iVoucherOrderService;

    @Autowired
    ISeckillVoucherService iSeckillVoucherService;

    @RabbitListener(queues = "work.queue",concurrency = "5-10")
    public void persistOrder( VoucherOrder voucherOrder)  throws InterruptedException{
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        Integer count = iVoucherOrderService.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count>0){
            log.error("下单一人超过一单");
            return;
        }
        //todo 写一个博客来优化该段
        boolean update = iSeckillVoucherService.update().setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();

        if(!update){
            log.error("库存不足");
            return;}
       iVoucherOrderService.save(voucherOrder);
        log.info("持久化数据成功");


    }
}

