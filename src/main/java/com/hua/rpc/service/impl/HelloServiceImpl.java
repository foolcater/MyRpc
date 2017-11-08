package com.hua.rpc.service.impl;

import com.hua.rpc.annotation.RpcService;
import com.hua.rpc.service.HelloService;
import com.hua.rpc.service.Person;

/**
 * Created by Administrator on 2017/10/30.
 */
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService{

    @Override
    public String hello(String name) {
        return "hello " + name;
    }

    @Override
    public String hello(Person person) {
        return "hello " + person.getFirstName() + " , " + person.getLastName();
    }
}
