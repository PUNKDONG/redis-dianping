package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    RedisIdWorker redisIdWorker;
    @Resource
    RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> IF_ORDER;

    static {
        IF_ORDER=new DefaultRedisScript<>();
        IF_ORDER.setLocation(new ClassPathResource("seckill.lua"));
        IF_ORDER.setResultType(Long.class);

    }
    @Override
    public Result seckillOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        Long result = stringRedisTemplate.execute(IF_ORDER,
                Collections.emptyList(),
                voucherId.toString(), userId.toString());

        int resultvalue = result.intValue();
        if(resultvalue!=0){
            if(resultvalue==1){
                return Result.fail("库存不足");
            }
            if(resultvalue==2){
                return Result.fail("用户已经下单");
            }
        }
        long orderID=redisIdWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(orderID);

        SendOrderToMQ(voucherOrder);
        CompletableFuture.runAsync(() -> SendOrderToMQ(voucherOrder), executorService);

        return Result.ok(orderID);
    }
    public void SendOrderToMQ(VoucherOrder voucherOrder){
        rabbitTemplate.convertAndSend("work.queue",voucherOrder);
    }
    @Override
    public void create1User1Order(VoucherOrder voucherOrder) {

    }
    //private BlockingQueue<VoucherOrder> oderTaskQueue=new ArrayBlockingQueue<VoucherOrder>(1024*1024);
    // 扩展线程池，避免线程池阻塞
    private ExecutorService executorService = Executors.newFixedThreadPool(10); // 线程池数量根据实际情况调整

}
