package com.hua.rpc.client.proxy;

import com.hua.rpc.client.RPCFuture;

public interface IAsyncObjectProxy {

    public RPCFuture call(String funcName, Object... args);
}
