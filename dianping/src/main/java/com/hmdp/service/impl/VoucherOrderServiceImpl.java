//package com.hmdp.service.impl;
//
//import cn.hutool.core.bean.BeanUtil;
//import com.hmdp.dto.Result;
//import com.hmdp.entity.SeckillVoucher;
//import com.hmdp.entity.User;
//import com.hmdp.entity.VoucherOrder;
//import com.hmdp.mapper.VoucherMapper;
//import com.hmdp.mapper.VoucherOrderMapper;
//import com.hmdp.service.ISeckillVoucherService;
//import com.hmdp.service.IVoucherOrderService;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.hmdp.utils.RedisConstants;
//import com.hmdp.utils.RedisIdWorker;
//import com.hmdp.utils.SimpleRedisLock;
//import com.hmdp.utils.UserHolder;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.aop.framework.AopContext;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.data.redis.connection.stream.*;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.*;
//
///**
// * <p>
// *  服务实现类
// * </p>
// *
// * @author 虎哥
// * @since 2021-12-22
// */
//@Service
//public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
//    @Resource
//    private ISeckillVoucherService iSeckillVoucherService;
//    @Resource
//    RedisIdWorker redisIdWorker;
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;
//    @Resource
//    RedissonClient redissonClient;
//    //private BlockingQueue<VoucherOrder> oderTaskQueue=new ArrayBlockingQueue<VoucherOrder>(1024*1024);
//    private static final DefaultRedisScript<Long> IFOder_SCRIPT;
//    static {
//        IFOder_SCRIPT = new DefaultRedisScript<>();
//        IFOder_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
//        IFOder_SCRIPT.setResultType(Long.class);
//    }
//    private static final ExecutorService SECKILL_POLL=Executors.newSingleThreadExecutor();
//    @PostConstruct
//    public void init(){
//        //SECKILL_POLL.submit(new VoucherOrderHandelTask());
//    }
//    private class VoucherOrderHandelTask implements Runnable{
//        @Override
//        public void run() {
//            while(true){
//                try {
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create("StreamOrder", ReadOffset.lastConsumed())
//                    );
//                    if (list  == null || list .isEmpty()) {
//                        continue;
//                    }
//                    MapRecord<String, Object, Object> lastMessage = list.get(0);
//                    Map<Object, Object> value = lastMessage.getValue();
//                    VoucherOrder voucherOrder  = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//
//                    handleVoucherOrder(voucherOrder);
//
//                    stringRedisTemplate.opsForStream().acknowledge("StreamOrder","g1",lastMessage.getId());
//                } catch (Exception e) {
//                    log.error("异常");
//                    handlePendingList();
//                }
//
//            }
//        }
//    }
//
//    private void handlePendingList() {
//        while(true){
//            try {
//                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                        Consumer.from("g1", "c1"),
//                        StreamReadOptions.empty().count(1),
//                        StreamOffset.create("StreamOrder", ReadOffset.from("0"))
//                );
//                if (list  == null || list.isEmpty()) {
//                    break;
//                }
//                MapRecord<String, Object, Object> lastMessage = list.get(0);
//                Map<Object, Object> value = lastMessage.getValue();
//                VoucherOrder voucherOrder  = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//                handleVoucherOrder(voucherOrder);
//
//                stringRedisTemplate.opsForStream().acknowledge("StreamOrder","g1",lastMessage.getId());
//            } catch (Exception e) {
//                log.error("pending 异常");
//                try {
//                    Thread.sleep(50);
//                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//
//        }
//    }
//
//    /* private class VoucherOrderHandelTask implements Runnable{
//         @Override
//         public void run() {
//             while(true){
//                 try {
//                     VoucherOrder voucherOrder = oderTaskQueue.take();
//                     handleVoucherOrder(voucherOrder);
//                 } catch (InterruptedException e) {
//                     log.error("异常");
//                     throw new RuntimeException(e);
//                 }
//
//             }
//         }
//     }*/
//    private  IVoucherOrderService proxy;
//    private void handleVoucherOrder(VoucherOrder voucherOrder) {
//        Long userid = voucherOrder.getUserId();
//
//        RLock lock = redissonClient.getLock(RedisConstants.VoucherOder_KEY + userid);
//        boolean islock = false;
//
//        islock = lock.tryLock();
//
//        if(!islock){
//            log.error("开挂是吧");
//            return;
//        }
//        try {
//            proxy.create1User1Order(voucherOrder);
//        } finally {
//            //
//           lock.unlock();
//        }
//
//    }
//
//    @Override
//    public Result seckillOrder(Long voucherId) {
//        Long Userid = UserHolder.getUser().getId();
//        long nextId = redisIdWorker.nextId(RedisConstants.VoucherOder_KEY);
//        Long result = stringRedisTemplate
//                .execute(IFOder_SCRIPT, Collections.emptyList(), voucherId.toString(), Userid.toString(),String.valueOf(nextId));
//        int r = result.intValue();
//        if(r==1){
//           return Result.fail("库存不足");
//        }
//        if(r==2){
//            return Result.fail("一个用户只能一个单");
//        }
//        //如果都过了，那么就可以创建新的订单，并放到阻塞队列中
//
//      /*  VoucherOrder voucherOder = new VoucherOrder();
//        voucherOder.setVoucherId(voucherId);
//        voucherOder.setId(nextId);
//        voucherOder.setUserId(UserHolder.getUser().getId());
//        long nextId = redisIdWorker.nextId(RedisConstants.VoucherOder_KEY);
//        oderTaskQueue.add(voucherOder);*/
//
//        proxy = (IVoucherOrderService)AopContext.currentProxy();
//        return Result.ok( );
//    }
//
////    public Result seckillOrder(Long voucherId) {
////        //1.通过id查优惠券
////        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);
////        //查到了，检查优惠卷时间
////        LocalDateTime now = LocalDateTime.now();
////        if (!(now.isAfter(seckillVoucher.getBeginTime())&&now.isBefore(seckillVoucher.getEndTime()))) {
////            return Result.fail("优惠券未到抢购时间");
////             }
////        //再检查优惠卷是否有库存
////        if(seckillVoucher.getStock()<=0){
////            return Result.fail("没库存");
////        }
////        Long userid = UserHolder.getUser().getId();
//////        SimpleRedisLock redisLock=new SimpleRedisLock(RedisConstants.VoucherOder_KEY+userid,stringRedisTemplate);
//////        boolean islock = redisLock.lock(10000L);
////        RLock lock = redissonClient.getLock(RedisConstants.VoucherOder_KEY + userid);
////        boolean islock = false;
////        try {
////            islock = lock.tryLock(1L, TimeUnit.SECONDS);
////        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
////        }
////        if(!islock){
////            return Result.fail("开挂是吧");
////        }
////        try {
////            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
////            return proxy.create1User1Order(voucherId);
////        } finally {
////            //
////           lock.unlock();
////        }
////
////
////    }
//    @Transactional
//    public void create1User1Order(VoucherOrder voucherOrder) {
//        //一人只能下一单
//        Long userid = voucherOrder.getUserId();
//        Long voucherId = voucherOrder.getVoucherId();
//
//        int  count = query().eq("user_id", userid).eq("voucher_id", voucherId).count();
//        if(count>0){
//           log.error("用户已经下过单了");
//           return;
//        }
//        //如果有进入下单流程，对库存减少1
//        boolean success = iSeckillVoucherService.update()
//                .setSql("stock = stock-1").eq("voucher_id", voucherId).gt("stock",0).update();
//        //返回生成的
//        if(!success){
//            log.error("sql判断不成功，乐观锁锁住了");
//            return;
//        }
//        //创建order
//
//        save(voucherOrder);
//
//
//    }
//}
