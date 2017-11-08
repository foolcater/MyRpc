package com.hua.rpc.server;

import com.hua.rpc.client.HelloService;

import java.lang.reflect.Proxy;

/**
 * Created by Administrator on 2017/11/3.
 */
public class Mian {

    public static <T> T create(Class<T> interfaceClass){
        return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new ObjectProxy<T>(interfaceClass));
    }

    public static <T> T createProxy(Class<T> tClass){
        Object o = null;
        int i = 0;
        for (Class clazz : tClass.getInterfaces()){
            o = (T)create(clazz);
            System.out.println(++i);
        }
        return (T) o;
    }

    public static void main(String[] args) throws Exception{
        HelloService helloService = Mian.createProxy(HelloServiceImpl.class);
        helloService.hello("哇哈哈哈哈");
    }
}
