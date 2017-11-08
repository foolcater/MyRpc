package com.hua.rpc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2017/11/7.
 */
public class HuaHello {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuaHello.class);

    @Resource(name = "HelloService")
    private HelloService helloService;

    public void huaSayHello(String name){
        String result = helloService.hello(name);
        LOGGER.info("hello service result :{}", result);
    }
}
