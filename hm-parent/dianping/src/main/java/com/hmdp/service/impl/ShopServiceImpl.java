package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    CacheClient cacheClient;;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Result queryByid(Long id) {
        //首先在redis里面查询缓存
         Shop shop = cacheClient.queryByidWithLogicTimeOut(id,Shop.class,this::getById,RedisConstants.CACHE_SHOP_KEY,10L,TimeUnit.SECONDS);
//
        if (shop == null) {
            return  Result.fail("店铺不存在");
        }
        return Result.ok(shop);

    }


//    public Shop queryByidWithMuteX(Long id)   {
//        //首先在redis里面查询缓存,这里通过互斥锁解决缓存击穿
//        //缓存击穿叫做热点key问题，也就是key失效之后大量对数据操作，然后数据库回写过程需要用互斥锁锁住
//        String cacheShop = stringRedisTemplate.opsForValue().get("cache:shop" + id);
//        //查询到了直接返回,以下判断首先判断是否为空，然后判断是否存在空字符串
//        //如果查
//        if(StrUtil.isNotBlank(cacheShop)){
//            Shop shopbean = JSONUtil.toBean(cacheShop, Shop.class);
//            return  shopbean;
//        }
//        if(cacheShop!=null){
//            //这里不为null就是空字符
//            return null;
//        }
//        //没查询到就从数据库里面查，然后放到redis里面
//        //到这里是redis没有数据了，那么我们再对数据库进行操作并回写的时候，我们要加锁
//        //1.获取互斥锁
//        if(!trylock("mutex:shop" + id)){
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//
//            queryByidWithMuteX(id);
//        };
//        //2.获取失败就重新循环
//        //3.判断是否redis有数据，如果有就返回
//        cacheShop = stringRedisTemplate.opsForValue().get("cache:shop" + id);
//        //查询到了直接返回,以下判断首先判断是否为空，然后判断是否存在空字符串
//        //如果查
//        if(StrUtil.isNotBlank(cacheShop)){
//            Shop shopbean = JSONUtil.toBean(cacheShop, Shop.class);
//            return  shopbean;
//        }
//        //4.如果没有就对数据库操作
//        //5.释放锁
//        Shop shopbySql = getById(id);
//        System.out.println("查了数据库1次");
//        if(shopbySql == null){
//            stringRedisTemplate.opsForValue().set("cache:shop" + id, "",2L, TimeUnit.MINUTES);
//            return null;
//        }
//        stringRedisTemplate.opsForValue().set("cache:shop" + id, JSONUtil.toJsonStr(shopbySql),30L, TimeUnit.MINUTES);
//        // 数据库也不存在就返回异常
//
//        unlock("mutex:shop" + id);
//        return  shopbySql;
//    }
//
//
    @Override
    @Transactional
    public Result updateWithDeleteCache(Shop shop) {
        //首先更新数据库然后再生成缓存
        if(shop.getId() == null){
            return Result.fail("这个id不存在");
        }
        updateById(shop);
        //然后删除缓存，如果没删除成功，那么下次查到的仍然是缓存中的错误数据
        stringRedisTemplate.delete("cache:shop" + shop.getId());
        return Result.ok("成功删除");
    }

}
