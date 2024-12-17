package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendcode(String phone, HttpSession session) {
        //1.校验
        if(RegexUtils.isPhoneInvalid(phone)){
           return Result.fail("手机号不对");
        }
        //2.生成
        String code= RandomUtil.randomNumbers(6);
        //3.保存到session
        stringRedisTemplate.opsForValue().set("login:code"+phone ,code,2, TimeUnit.MINUTES);
       // session.setAttribute(" code",code);
        //4.发送code,用log输出代替
        log.debug("code is {}",code);
        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式不对");
        }

        //2.校验验证码是否等于session验证码
        String cacheCode = stringRedisTemplate.opsForValue().get("login:code" + phone);
        String code =loginForm.getCode();
        if(cacheCode==null||!cacheCode.toString().equals(code)){
            return Result.fail("code error");
        }
        //3.手机号是否存在，不存在，则新建用户到数据库，并保存到session中
        User user = query().eq("phone", loginForm.getPhone()).one();
        if(user==null){
            user=creatUserWithPhone(phone);
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        Map<String, Object> usermap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((filedname,filedvalue)->filedvalue.toString())
        );
        //session.setAttribute("user",userDTO);
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForHash().putAll("login:token:"+token,usermap );
        stringRedisTemplate.expire("login:token:"+token,30, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User creatUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user"+RandomUtil.randomNumbers(6));
        save(user);
        return user;
    }
}
