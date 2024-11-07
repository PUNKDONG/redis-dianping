package com.hmdp.service.impl;

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
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override

    public Result seckillOrder(Long voucherId) {
        //1.通过id查优惠券
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);
        //查到了，检查优惠卷时间
        LocalDateTime now = LocalDateTime.now();
        if (!(now.isAfter(seckillVoucher.getBeginTime())&&now.isBefore(seckillVoucher.getEndTime()))) {
            return Result.fail("优惠券未到抢购时间");
             }
        //再检查优惠卷是否有库存
        if(seckillVoucher.getStock()<=0){
            return Result.fail("没库存");
        }
        Long userid = UserHolder.getUser().getId();
        SimpleRedisLock redisLock=new SimpleRedisLock(RedisConstants.VoucherOder_KEY+userid,stringRedisTemplate);
        boolean islock = redisLock.lock(10000L);
        if(!islock){
            return Result.fail("开挂是吧");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.create1User1Order(voucherId);
        } finally {
            redisLock.unlock();
        }


    }
    @Transactional
    public Result create1User1Order(Long voucherId) {
        //一人只能下一单
        Long userid = UserHolder.getUser().getId();

        int  count = query().eq("user_id", userid).eq("voucher_id", voucherId).count();
        if(count>0){
            return Result.fail("用户已经下过单了");
        }
        //如果有进入下单流程，对库存减少1
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock-1").eq("voucher_id", voucherId).gt("stock",0).update();
        //返回生成的
        if(!success){
            return Result.fail("最后的sql语句有问题");
        }
        //创建order
        long nextId = redisIdWorker.nextId(RedisConstants.VoucherOder_KEY);
        VoucherOrder voucherOder = new VoucherOrder();
        voucherOder.setVoucherId(voucherId);
        voucherOder.setId(nextId);
        voucherOder.setUserId(UserHolder.getUser().getId());
        save(voucherOder);

        return Result.ok(nextId);
    }
}
