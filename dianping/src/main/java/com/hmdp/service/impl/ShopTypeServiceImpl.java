package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public Result queryListUseCache() {
        //首先查看redis里面有没有，然后在查看数据库
        List<String> alltype = stringRedisTemplate.opsForList().range("cache:type", 0, -1);
        //出现一个问题就是List<String>要转换成List<ShopType>
        if (!alltype.isEmpty()) {
            List<ShopType> shopTypes=new ArrayList<>() ;
            for (String type : alltype) {
                ShopType bean = JSONUtil.toBean(type, ShopType.class);
                shopTypes.add(bean);
            }
            return Result.ok(shopTypes);
        }
        //然后是查数据库
        List<ShopType> listDatabase = query().list();
        if ( listDatabase.isEmpty()) {
            return Result.fail("数据库也没有");
        }
        //否则直接把数据库的存到redis中

        for (ShopType shopType : listDatabase) {
            stringRedisTemplate.opsForList().rightPush("cache:type",JSONUtil.toJsonStr(shopType));
        }

        return Result.ok(listDatabase);
    }
}
