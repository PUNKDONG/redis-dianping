package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
      return blogService.saveAndFeed(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
//        blogService.update()
//                .setSql("liked = liked + 1").eq("id", id).update();
//
//        return Result.ok();
        return blogService.updateLike(id);
    }
    @GetMapping("/likes/{id}")
    public Result getLikes(@PathVariable("id") Long id) {
        return blogService.getLikes(id);
    }
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据用户查询
       return  blogService.queryBlogHotBlog(current);
    }
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        // 根据用户查询

        return blogService.queryBlogById(id);
    }
    @GetMapping("/of/user")
    public Result queryBlogByUserId( @RequestParam(value = "current", defaultValue = "1") Integer current,
                                     @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> blogPage=blogService.query().eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> pageList = blogPage.getRecords();

        return Result.ok(pageList);
    }
    @GetMapping("/of/follow")
    public  Result queryFollowBlog( @RequestParam(value = "lastId" ) Long maxid,@RequestParam(value = "offset",defaultValue = "0") Integer offset){
    return  blogService.queryFollowBlog(maxid,offset);
    }



}