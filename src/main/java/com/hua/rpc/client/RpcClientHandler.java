package com.hua.rpc.client;

import com.hua.rpc.protocol.RpcRequest;
import com.hua.rpc.protocol.RpcResponse;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class RpcClientHandler extends ChannelHandlerAdapter{
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String, RPCFuture> predingRPC = new ConcurrentHashMap<String, RPCFuture>();

    private volatile Channel channel;
    private SocketAddress remotePeer;

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        LOGGER.info("建立连接成功");
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcResponse response = (RpcResponse) msg;
        String requestId = response.getRequestId();
        RPCFuture rpcFuture = predingRPC.get(requestId);
        if (rpcFuture != null){
            predingRPC.remove(requestId);
            rpcFuture.done(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
    }

    public void close(){
        LOGGER.info("客户端主动关闭连接");
        if (channel != null)
            this.channel.close();
    }

    public RPCFuture sendRequest(RpcRequest request){
        final CountDownLatch latch = new CountDownLatch(1);
        RPCFuture rpcFuture = new RPCFuture(request);
        predingRPC.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                LOGGER.info("数据请求发送成功");
                latch.countDown();
            }
        });
        try {
            latch.await();
        }catch (InterruptedException e){
            LOGGER.error(e.getMessage());
        }
        return rpcFuture;
    }
}
