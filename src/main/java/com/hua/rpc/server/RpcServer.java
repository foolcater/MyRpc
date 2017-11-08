package com.hua.rpc.server;

import com.hua.rpc.annotation.RpcService;
import com.hua.rpc.protocol.RpcDecoder;
import com.hua.rpc.protocol.RpcEncoder;
import com.hua.rpc.protocol.RpcRequest;
import com.hua.rpc.protocol.RpcResponse;
import com.hua.rpc.registry.ServiceRegistry;
import com.hua.rpc.registry.ZookeeperService;
import com.hua.rpc.util.Utils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcServer implements ApplicationContextAware, InitializingBean{

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private Map<String, Object> handlerMap = new HashMap<String, Object>();
    private int serverPort;
    private ZookeeperService zookeeperService;

    private static ThreadPoolExecutor threadPoolExecutor;

    public RpcServer(int serverPort, ZookeeperService zookeeperService) {
        this.serverPort = serverPort;
        this.zookeeperService = zookeeperService;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)){
            for (Object serviceBean : serviceBeanMap.values()){
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName, serviceBean);
            }
            registerRemoteService();
        }
    }

    public void afterPropertiesSet() throws Exception {
        startServer();
    }

    private void startServer() throws Exception{
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    LOGGER.info( Thread.currentThread().getName() + "开始启动netty");
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            .handler(new LoggingHandler(LogLevel.DEBUG))
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    socketChannel.pipeline()
                                            .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                            .addLast(new RpcDecoder(RpcRequest.class))
                                            .addLast(new RpcEncoder(RpcResponse.class))
                                            .addLast(new RpcHandler(handlerMap));
                                }
                            });
                    InetAddress localHost = Utils.getLocalHostLANAddress();

                    ChannelFuture future = bootstrap.bind(localHost,serverPort).sync();
                    LOGGER.info("Server started on port {}", serverPort);
                    future.channel().closeFuture().sync();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        });
        t.setName("netty server thread");
        t.start();
    }

    private void registerRemoteService(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress address = Utils.getLocalHostLANAddress();
                    String data = address.getHostAddress() + ":" + serverPort;
                    for (String className : handlerMap.keySet()){
                        zookeeperService.createProviderNode(className, data);
                        LOGGER.info("successful register service:{}, data:{}", className, data);
                    }
                }catch (Exception e){
                    LOGGER.error("register service error", e);
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.setName("connect-zookeeper-server-thread");
        thread.start();
    }

    public static void submit(Runnable task){
        if (threadPoolExecutor == null){
            synchronized (RpcServer.class){
                if (threadPoolExecutor == null){
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16,600L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }
}
