package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    private final StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    public FollowServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result followUser(Long followUserId, Boolean ifFollow) {
        Long userId = UserHolder.getUser().getId();
        if(ifFollow){
        //true就关注
            Follow follow = new Follow();
            follow.setUserId(userId) ;
            follow.setFollowUserId(followUserId) ;
            boolean ifsave = save(follow);
            if(ifsave){
                stringRedisTemplate.opsForSet().add("follow:List"+userId, String.valueOf(followUserId));
            }
        }
        else{
            boolean ifremove = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if(ifremove){
            stringRedisTemplate.opsForSet().remove("follow:List"+userId,String.valueOf(followUserId));}
        }

        return Result.ok();
    }

    @Override
    public Result ifFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        if(count == 0){
            return Result.ok(false);
        }
        else {
            return Result.ok(true);
        }

    }

    @Override
    public Result commonFollow(Long followUserId) {
        Long UserId = UserHolder.getUser().getId();
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect("follow:List"+UserId,"follow:List"+followUserId);
        if(intersect ==null || intersect.isEmpty()){
            return Result.ok( );
        }
        List<UserDTO> collect = userService.listByIds(intersect).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());



        return Result.ok(collect);
    }

}
