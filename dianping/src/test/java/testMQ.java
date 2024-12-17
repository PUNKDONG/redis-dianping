import com.hmdp.HmDianPingApplication;
import com.hmdp.service.impl.ShopServiceImpl;

import com.hmdp.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = HmDianPingApplication.class)
public class testMQ {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Test
    public void testQueue(){
        String queuename="work.queue";


        for(int i=0;i<50;i++){
            String msg="Hello World! "+i;
            rabbitTemplate.convertAndSend(queuename,msg);
        }
    }
}
