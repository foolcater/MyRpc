package com.hua.rpc.client;

import com.hua.rpc.protocol.RpcDecoder;
import com.hua.rpc.protocol.RpcEncoder;
import com.hua.rpc.protocol.RpcRequest;
import com.hua.rpc.protocol.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;


public class RpcClientInitializer extends ChannelInitializer<SocketChannel>{

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new RpcEncoder(RpcRequest.class))
                .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                .addLast(new RpcDecoder(RpcResponse.class))
                .addLast(new RpcClientHandler());
    }
}
