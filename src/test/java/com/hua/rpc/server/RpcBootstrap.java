package com.hua.rpc.server;

import com.hua.rpc.service.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcBootstrap.class);

    public static void main(String[] args){
        LOGGER.info("开始启动");
        ApplicationContext applicationContext =  new ClassPathXmlApplicationContext("server-spring.xml");
       // ApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyBeanDefinitionRegistryPostProcessor.class);
        LOGGER.info("启动完成");
        HelloService helloService = applicationContext.getBean(HelloService.class);
        LOGGER.info("获取bean的实例");
        String result = helloService.hello("world");
        LOGGER.info("执行结果:{}", result);
    }
}
