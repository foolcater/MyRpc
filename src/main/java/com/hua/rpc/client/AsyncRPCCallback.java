package com.hua.rpc.client;

/**
 * @author hua
 * @date 2017-10-28
 */
public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);
}
