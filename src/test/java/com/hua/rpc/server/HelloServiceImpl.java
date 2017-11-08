package com.hua.rpc.server;

import com.hua.rpc.annotation.RpcService;
import com.hua.rpc.client.HelloService;
import com.hua.rpc.client.Person;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService{

    @Override
    public String hello(String name) {
        return "hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "hello! " + person.getFirstName() + " " + person.getLastName();
    }
}
