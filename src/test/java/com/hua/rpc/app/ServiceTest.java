package com.hua.rpc.app;

import com.hua.rpc.client.HelloService;
import com.hua.rpc.client.RpcClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client-spring.xml")
public class ServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTest.class);
    @Autowired
    private RpcClient rpcClient;

    @Test
    public void helloTest1(){
        HelloService helloService = rpcClient.create(HelloService.class);
        for (int i = 0; i<10; i++){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = helloService.hello(new Random().nextInt(100) + "world");
                    LOGGER.info(Thread.currentThread().getName() + " 获取到结果:{}", result);
                    System.out.println(Thread.currentThread().getName() + " 获取到结果:{}" + result);
                }
            });
            t.start();
        }
        try {
            Thread.currentThread().sleep(500000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        LOGGER.info("主线程结束");
    }
}
