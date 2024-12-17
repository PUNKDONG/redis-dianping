package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private UserServiceImpl userService ;
    @Resource
    private IFollowService followService ;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);

        if(blog == null){
            return Result.fail("笔记不存在");
        }
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        Boolean blogLiked = isBlogLiked(id);
        blog.setIsLike(blogLiked);
        return Result.ok(blog);
    }
    public Boolean isBlogLiked(Long id){
        UserDTO getuser = UserHolder.getUser();
        if(getuser==null){
            return false;
        }
        Long nowUserID = getuser.getId();

        Double score = stringRedisTemplate.opsForZSet().score("Blog:Like:List" + id, String.valueOf(nowUserID));
        boolean isMember = score != null;
        if (Boolean.TRUE.equals(isMember)) {
            //存在
            return true;
        }
        else return false;

    }
    @Override
    @Transactional
    public Result updateLike(Long id) {
        Long nowUserID = UserHolder.getUser().getId();

        Double score = stringRedisTemplate.opsForZSet().score("Blog:Like:List" + id, String.valueOf(nowUserID));
        boolean isMember = score != null;

        //1.首先每条笔记维护一个set数组
        if(Boolean.TRUE.equals(isMember)){
            //如果存在，就把自己删除，然后数据库减少1
            stringRedisTemplate.opsForZSet().remove("Blog:Like:List" + id,String.valueOf(nowUserID));
           update().setSql("liked = liked - 1").eq("id", id).update();
        }
        else {
            stringRedisTemplate.opsForZSet().add("Blog:Like:List" + id, String.valueOf(nowUserID),System.currentTimeMillis());
            update().setSql("liked = liked + 1").eq("id", id).update();
        }

        //3.如果不存在就，增加like+1
        return Result.ok(isMember);
    }

    @Override
    public Result queryBlogHotBlog(Integer current) {
        Page<Blog> page =  query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            Boolean blogLiked = isBlogLiked(blog.getId());
            blog.setIsLike(blogLiked);
        });
        return Result.ok(records);
    }

    @Override
    public Result getLikes(Long id) {
        Set<String> members = stringRedisTemplate.opsForZSet().range("Blog:Like:List" + id, 0, 4);

        List<UserDTO> userDTOS = new ArrayList<>();
        for (String  member : members) {
            long l = Long.parseLong(member);
            User user = userService.getById(l);
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            userDTOS.add(userDTO);
        }
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveAndFeed(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean ifsave = save(blog);
        if(!ifsave){
            return  Result.fail("新增失败");
        }

        //获取所有的关注
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            String key = "feed:" + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        stringRedisTemplate.opsForSet();
        // 返回id
        return Result.ok(blog.getId());


    }

    @Override
    public Result queryFollowBlog(Long maxid, Integer offset) {
        UserDTO user = UserHolder.getUser();
        Long userId = user.getId();
        String key = "feed:" + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate
                .opsForZSet().reverseRangeByScoreWithScores(key, 0, maxid, offset, 2);

        if (typedTuples == null) {
            return Result.ok();
        }
        long mintime=0;
        int os=1;
        List<Blog> blogs = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //获得值，我们要把对应的值查出来然后返回
            long time = typedTuple.getScore().longValue();
            String followBlogId = typedTuple.getValue();
            Blog followBlog = getById(followBlogId);
            Long followId  = followBlog .getUserId();
            User user1 = userService.getById(followId);
            followBlog.setIcon(user1.getIcon());
            followBlog.setName(user1.getNickName());
            Boolean blogLiked = isBlogLiked(followId);
            followBlog.setIsLike(blogLiked);
            blogs.add(followBlog);
            if(mintime==time){
                os++;
            }
            else{
                mintime=time;
                os=1;
            }
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(mintime);

        return Result.ok(scrollResult);
    }


}
