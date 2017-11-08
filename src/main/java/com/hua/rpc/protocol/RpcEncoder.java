package com.hua.rpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder{

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass){
        this.genericClass = genericClass;
    }

    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if (genericClass.isInstance(o)){
            byte[] data = SerializationUtil.serialize(o);
            //byte[] data = JsonUtil.serialize(in); // Not use this, have some bugs
            byteBuf.writeInt(data.length);
            byteBuf.writeBytes(data);
        }
    }
}
