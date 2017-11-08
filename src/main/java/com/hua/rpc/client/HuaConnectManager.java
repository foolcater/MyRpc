package com.hua.rpc.client;

import com.hua.rpc.registry.ZookeeperService;
import com.hua.rpc.spring.SpringContextUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2017/10/31.
 */
public class HuaConnectManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuaConnectManager.class);
    private static volatile HuaConnectManager instance = null;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16,32,500L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(65536));
    //key对应服务名称即className value 对应服务地址列表
    private  ConcurrentHashMap<String, List<String>> serviceHost = new ConcurrentHashMap<String, List<String>>();
    //key 对应服务地址列表 val 为handler实例
    private  ConcurrentHashMap<String, RpcClientHandler> serviceHandler = new ConcurrentHashMap<String, RpcClientHandler>();
    //所有的服务地址列表
    private List<String> ALLHost = new ArrayList<String>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition conneted = lock.newCondition();
    private long connectTimeoutMills = 6000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRunning = false;

    private HuaConnectManager(){

    }

    public static HuaConnectManager getInstance(){
        if (instance == null){
            synchronized (HuaConnectManager.class){
                if (instance == null){
                    instance = new HuaConnectManager();
                }
            }
        }
        return instance;
    }

    /**
     * 发现服务
     * @param className
     */
    public void findService(String className){
        LOGGER.info("发现服务:" + className);
        if (serviceHost.get(className) != null)
            return;
        ZookeeperService zookeeperService = (ZookeeperService) SpringContextUtil.getBean("zookeeperService");
        zookeeperService.findService(className);

    }

    /**
     * 更新服务器列表
     * @param className
     * @param hostList
     */
    public void updateServiceHost(String className, List<String> hostList){
        LOGGER.info("更新服务:{}，服务地址:{}", className, hostList);
        if (serviceHost.get(className) == null){
            serviceHost.put(className, hostList);
            for (String host : hostList){
                if (!ALLHost.contains(host)){
                    ALLHost.add(host);
                    connectServer(host);
                }
            }
        }else {
            List<String> cunrrentHost = serviceHost.get(className);
            for (String node : cunrrentHost){
                if (!hostList.contains(node)){
                    cunrrentHost.remove(node);
                    RpcClientHandler handler = serviceHandler.get(node);
                    if (handler != null) {
                        handler.close();
                        serviceHandler.remove(node);
                        ALLHost.remove(node);
                    }
                }
            }
            for (String node : hostList){
                if (!cunrrentHost.contains(node)){
                    cunrrentHost.add(node);
                }
            }
            for (String node : cunrrentHost){
                if (!ALLHost.contains(node)){
                    ALLHost.add(node);
                    connectServer(node);
                }
            }
        }

    }

    /**
     * 连接服务器
     * @param host
     */
    private void connectServer(String host){
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup).channel(NioSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .handler(new RpcClientInitializer());
                String[] array = host.split(":");
                ChannelFuture future = b.connect(array[0], Integer.parseInt(array[1]));
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        LOGGER.info("successful connect to remote server, remote peer = " +host);
                        RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
                        addHandler(handler, host);
                    }
                });
            }
        });
    }

    private void addHandler(RpcClientHandler handler, String host){
        serviceHandler.put(host, handler);
        singleAvailableHandler();
    }

    private void singleAvailableHandler(){
        lock.lock();
        try {
            conneted.signalAll();
        }finally {
            lock.unlock();
        }
    }

    public RpcClientHandler getConnection(String className){
        List<String> hostList = serviceHost.get(className);
        LOGGER.info("hostList={}", hostList);
        LOGGER.info("serviceHost = {}", serviceHost);
        LOGGER.info("serviceHandler={}", serviceHandler);
        if (hostList == null || hostList.isEmpty()){
            waitingForHandler();
            hostList = serviceHost.get(className);
        }
        if (hostList== null || hostList.isEmpty()){
            return null;
        }
        int index = (roundRobin.getAndAdd(1) + hostList.size()) % hostList.size();
        String host = hostList.get(index);
        LOGGER.info("选中的IP:{}", host);
        RpcClientHandler handler = serviceHandler.get(host);
        LOGGER.info("handler ip : {}", handler.getRemotePeer());
        return handler;
    }

    private void waitingForHandler() {
        lock.lock();
        try {
            conneted.await(this.connectTimeoutMills, TimeUnit.MILLISECONDS);
        }catch (InterruptedException e){

        }finally {
            lock.unlock();
        }
    }
}
