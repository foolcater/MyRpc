package com.hua.rpc.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2017/11/3.
 */
public class ObjectProxy<T> implements InvocationHandler{

    private Class<T> clazz;

    public ObjectProxy(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("lallalalla");
        return null;
    }
}
