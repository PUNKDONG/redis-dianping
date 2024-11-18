package com.hmdp.controller;


import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // TODO 发送短信验证码并保存验证码
        return   userService.sendcode(phone,session) ;

    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // TODO 实现登录功能
       return userService.login(loginForm,session);

    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        // TODO 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        if(user == null){
            return Result.ok(user);

        }
        Long userId = user.getId();
        User userALL = userService.getById(userId);

        BeanUtils.copyProperties(userALL,user);
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long userId){
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        return Result.ok(userDTO);

    }
    @PostMapping("/sign")
    public Result sendSign( ){
        LocalDateTime now = LocalDateTime.now();
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        Long id = UserHolder.getUser().getId();
        String key ="sign"+ id.toString() + ":" + yyyyMM;
        int day = now.getDayOfMonth();
        stringRedisTemplate.opsForValue().setBit(key,day-1,true);

        return Result.ok();
    }
    @PostMapping("/signcount")
    public Result  Signcount( ){
        LocalDateTime now = LocalDateTime.now();
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        Long id = UserHolder.getUser().getId();
        String key ="sign"+ id.toString() + ":" + yyyyMM;
        int day = now.getDayOfMonth();
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0)
        );
        Long allday = result.get(0);
        int lianxushijian = 0;
        while(true){
            if((allday&1)==0){
                break;
            }
            else {
                lianxushijian++;

            }
            allday=allday>>1;
        }

        return Result.ok(lianxushijian);
    }
}
