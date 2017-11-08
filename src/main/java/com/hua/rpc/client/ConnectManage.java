package com.hua.rpc.client;

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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC connect Manager of zookeeper
 * @author hua
 */
public class ConnectManage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectManage.class);
    private volatile static ConnectManage connectManage;

    EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16,16,500L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<RpcClientHandler>();
    private Map<InetSocketAddress, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<InetSocketAddress, RpcClientHandler>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    protected long connectTimeoutMills = 6000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRunning = true;

    private ConnectManage(){ }

    /**
     * 单例
     * @return
     */
    public static ConnectManage getInstance(){
        if (connectManage == null){
            synchronized (ConnectManage.class){
                if (connectManage == null)
                    connectManage = new ConnectManage();
            }
        }
        return connectManage;
    }

    public void updateConnectedServer(List<String> allServerAddress){
        if (allServerAddress != null){
            if (allServerAddress.size() > 0){
                // update local serverNode cache
                HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<InetSocketAddress>();
                for (int i=0; i<allServerAddress.size(); i++){
                    String[] array = allServerAddress.get(i).split(":");
                    if (array.length == 2){
                        String host = array[0];
                        int port =Integer.parseInt(array[1]);
                        final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
                        newAllServerNodeSet.add(remotePeer);
                    }
                }

                // add new server node
                for (final InetSocketAddress serverNodeAddress : newAllServerNodeSet){
                    if (!connectedServerNodes.keySet().contains(serverNodeAddress)){
                        connectServerNode(serverNodeAddress);
                    }
                }

                //close and remove invalid server nodes
                for (int i=0; i<connectedHandlers.size(); i++){
                    RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
                    SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
                    if (!newAllServerNodeSet.contains(remotePeer)){
                        LOGGER.info("Remote invalid server node " + remotePeer);
                        RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                        LOGGER.info("i am mot to close handler");
                        handler.close();
                        connectedHandlers.remove(remotePeer);
                        connectedHandlers.remove(connectedServerHandler);
                    }
                }
            }else { // no available server node (all server nodes are down)
                LOGGER.info("no available server node, all server nodes are down !!!");
                for (final RpcClientHandler connectedServerHandler : connectedHandlers){
                    SocketAddress remotePeer = connectedServerHandler.getRemotePeer();
                    RpcClientHandler handler = connectedServerNodes.get(remotePeer);
                    LOGGER.info(" handler closed");
                    handler.close();
                    connectedServerNodes.remove(connectedServerHandler);
                }
                connectedHandlers.clear();
            }
        }
    }

    private void connectServerNode(final InetSocketAddress remotePeer){
        threadPoolExecutor.submit(new Runnable() {
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup).channel(NioSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.DEBUG))
                        .handler(new RpcClientInitializer());
                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()){
                            LOGGER.info("Successfully connect to remote server, remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(handler);
                        }
                    }
                });
            }
        });
    }

    private void addHandler(RpcClientHandler handler){
        connectedHandlers.add(handler);
        InetSocketAddress remoteAddress = (InetSocketAddress) handler.getChannel().remoteAddress();
        connectedServerNodes.put(remoteAddress, handler);
        singleAvailableHandler();
    }

    private void singleAvailableHandler(){
        lock.lock();
        try {
            connected.signalAll();
        }finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException{
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMills, TimeUnit.MILLISECONDS);
        }finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler(){
        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();
        int size = handlers.size();
        while (isRunning && size <= 0){
            try {
                boolean available = waitingForHandler();
                if (available){
                    handlers =  (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();
                    size = handlers.size();
                }
            }catch (InterruptedException e){
                LOGGER.error("Waiting for available node is interrupted!", e);
                throw new RuntimeException("Con't connect any servers!", e);
            }
        }
        int index = (roundRobin.getAndAdd(1)+size) % size;
        return handlers.get(index);
    }

    public void stop(){
        isRunning = false;
        for (int i=0; i<connectedHandlers.size(); i++){
            RpcClientHandler connectedServerHandler = connectedHandlers.get(i);
            LOGGER.error("i am not how to close handler!");
            connectedServerHandler.close();
            //connectedServerHandler.close();
        }
        singleAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
