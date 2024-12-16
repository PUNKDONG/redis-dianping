//package com.hmdp;
//
//import com.hmdp.dto.UserDTO;
//import com.hmdp.entity.User;
//import com.hmdp.service.impl.ShopServiceImpl;
//
//import com.hmdp.service.impl.UserServiceImpl;
//import org.junit.jupiter.api.Test;
//
//import org.springframework.beans.BeanUtils;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.HashOperations;
//import org.springframework.data.redis.core.StringRedisTemplate;
//
//
//import javax.annotation.Resource;
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.Callable;
//import java.util.concurrent.FutureTask;
//import java.util.concurrent.TimeUnit;
///*
//为了支持 JMeter 的多用户并发登录压力测试，我们提供了一个脚本，用于向 Redis 中批量写入 1000 个用户的 token，并将这些 token 输出到文件供 JMeter 使用。 在测试单元中src/test/java/com/hmdp/HmDianPingApplicationTests.java中
//
//脚本功能
//该脚本会执行以下操作：
//
//从数据库中获取用户列表。
//为每个用户生成唯一的 token。
//将用户数据以 token 为键存储到 Redis。
//将所有 token 保存到 tokens.txt 文件中。
// */
//@SpringBootTest
//class HmDianPingApplicationTests {
//    @Resource
//    private  ShopServiceImpl shopService;
//
//    @Resource
//    private UserServiceImpl userService;
//    @Resource
//    StringRedisTemplate stringRedisTemplate;
//    @Test
//    public void excuteSave2(){
//        List<User> users= userService.list();
//        // 2. 打开文件，准备写入 token 供 JMeter 使用
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("tokens.txt"))) {
//            for (User user : users) {
//                // 3. 创建 UserDTO，并生成唯一 token
//                UserDTO userDTO = new UserDTO();
//                BeanUtils.copyProperties(user, userDTO);
//                String token = UUID.randomUUID().toString().replace("-", "");
//
//                // 4. 将 UserDTO 对象转换为 Map 并保存到 Redis
//                Map<String, Object> userMap = new HashMap<>();
//                userMap.put("nickName", userDTO.getNickName());
//                userMap.put("icon", userDTO.getIcon());
//                userMap.put("id", userDTO.getId().toString());
//
//                String redisKey = "login:token:" + token;
//                HashOperations<String, Object, Object> opsForHash = stringRedisTemplate.opsForHash();
//                opsForHash.putAll(redisKey, userMap);
//                stringRedisTemplate.persist(redisKey);
//
//                // 5. 将 token 写入文件
//                writer.write(token);
//                writer.newLine();
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("Token generation complete. Tokens saved to tokens.txt for JMeter testing.");
//    }
//    @Test
//    public void UVcount(){
//        String[] users = new String[1000];
//        for (int i = 0; i < 10000; i++) {
//            String s = "user" + i;
//            int j=i%1000;
//            users[j] = s;
//            if(j==999){
//                stringRedisTemplate.opsForHyperLogLog().add("testUV",users);
//            }
//        }
//        Long testUV = stringRedisTemplate.opsForHyperLogLog().size("testUV");
//        System.out.println(testUV);
//
//
//    }
//    public List<List<Integer>> generate(int numRows) {
//        ArrayList<List<Integer>> all=new  ArrayList<>();
//        ArrayList<Integer> fhang=new ArrayList<>();
//       fhang.add(1);
//        all.add(fhang);
//        for(int i=1;i<numRows;i++){
//            ArrayList<Integer> hang = new ArrayList<>();
//
//            hang.add(1);
//            for(int j=1;j<numRows-1;j++){
//                List<Integer> integers = all.get(all.size() - 1);
//                hang.add(integers.get(j-1)+integers.get(j));}
//            hang.add(1);
//            all.add(hang);
//        }
//        return all;
//    }
//
//}
//
