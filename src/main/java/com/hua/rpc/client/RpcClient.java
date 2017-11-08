package com.hua.rpc.client;

import com.hua.rpc.client.proxy.IAsyncObjectProxy;
import com.hua.rpc.client.proxy.ObjectProxy;
import com.hua.rpc.registry.ServiceDiscovery;
import com.hua.rpc.registry.ZookeeperService;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Rpc Client (create RPC proxy)
 * @author hua
 */
public class RpcClient {

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16,16,600L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(65536));


    public static <T> T create(Class<T> interfaceClass){
        //发现服务
        HuaConnectManager.getInstance().findService(interfaceClass.getName());
        //生成代理
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass));
    }

    public static <T>IAsyncObjectProxy createAsync(Class<T> interfaceClass){
        return new ObjectProxy<T>(interfaceClass);
    }


    public static void submit(Runnable task){
        threadPoolExecutor.submit(task);
    }

    public void stop(){
        threadPoolExecutor.shutdown();
    }
}
