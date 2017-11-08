package com.hua.rpc.main;

import com.hua.rpc.registry.ZookeeperService;
import com.hua.rpc.service.HahaService;
import com.hua.rpc.service.HelloService;
import com.hua.rpc.service.HuaHello;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Administrator on 2017/10/30.
 */
public class RpcBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcBootstrap.class);

    public static void main(String[] args) throws Exception{
        LOGGER.info("开始启动");
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:server-spring.xml");
        LOGGER.info("启动完成");
//        long time = System.currentTimeMillis();
//        while (true){
//            Thread.currentThread().sleep(10000);
//            if ((System.currentTimeMillis() - time)/1000 > 6000){
//                break;
//            }
//        }
//        HelloService helloService = (HelloService) applicationContext.getBean("HelloService");
//        LOGGER.info("获取bean实例完成");
//        Thread.currentThread().sleep(10000);
//        for (int i = 0; i<10; i++) {
//            try {
//                String result = helloService.hello("world!" + i);
//                LOGGER.info("执行结果:{}", result);
//            }catch (Exception e){
//                LOGGER.error("出现错误", e);
//            }
//        }
//        Thread.currentThread().sleep(50000);
//
//        for (int i=20; i<30; i++){
//            try {
//                String result = helloService.hello("world!" + i);
//                LOGGER.info("执行结果:{}", result);
//            }catch (Exception e){
//                LOGGER.error("出现错误", e);
//            }
//        }

        HuaHello hello = (HuaHello) applicationContext.getBean("huaService");
        Thread.currentThread().sleep(10000);
        for (int i=0; i<10; i++){
            try {
                hello.huaSayHello("world!_" + i);
            }catch (Exception e){
                LOGGER.error("exception", e);
            }
        }
    }
}
