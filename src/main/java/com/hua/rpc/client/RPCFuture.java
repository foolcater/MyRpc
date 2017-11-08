package com.hua.rpc.client;

import com.hua.rpc.protocol.RpcRequest;
import com.hua.rpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class RPCFuture implements Future<Object>{

    private static final Logger LOGGER = LoggerFactory.getLogger(RPCFuture.class);
    private Sync sync;
    private RpcRequest request;
    private RpcResponse response;

    private long startTime;
    private long responseTimeThreadhold = 5000;

    private List<AsyncRPCCallback> predingCallBacks = new ArrayList<AsyncRPCCallback>();
    private ReentrantLock lock = new ReentrantLock();

    public RPCFuture(RpcRequest request) {
        this.request = request;
        this.sync = new Sync();
        this.startTime = System.currentTimeMillis();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return sync.isDone();
    }

    public void done(RpcResponse response){
        this.response = response;
        LOGGER.info("请求Id=" + response.getRequestId()+" 获取到请求结果， result=" + response.getObject());
        sync.release(-1);
        invokeCallBacks();

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreadhold){
            LOGGER.warn("Service response time is too slow, Request id = " + response.getRequestId()+". Response Time = " + responseTime + "ms");
        }
    }

    public Object get() throws InterruptedException, ExecutionException {
        LOGGER.info(Thread.currentThread().getName() +  " 等待获取结果");
        sync.acquire(-1);
        LOGGER.info(Thread.currentThread().getName() + " 获取到锁，已获取结果");
        if (this.response != null){
            return this.response.getObject();
        }else
            return null;
    }

    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success){
            if (this.response != null){
                return this.response.getObject();
            }else {
                return null;
            }
        }else {
            throw new RuntimeException("Timeout Exception, Request id: "+ this.request.getRequestId()
                + ". Request class name: " + this.request.getClassName()
                + ". Request method: " + this.request.getMethodName());
        }
    }

    private void invokeCallBacks(){
        lock.lock();
        try {
            for (final AsyncRPCCallback callback : predingCallBacks){
                runCallback(callback);
            }
        }finally {
            lock.unlock();
        }
    }

    public RPCFuture addCallback(AsyncRPCCallback callback){
        lock.lock();
        try {
            if (isDone()){
                runCallback(callback);
            }else {
                this.predingCallBacks.add(callback);
            }
        }finally {
            lock.unlock();
        }
        return this;
    }

    private void runCallback(final AsyncRPCCallback callback){
        final RpcResponse response = this.response;
        RpcClient.submit(new Runnable() {
            public void run() {
                if (!response.isError()){
                    callback.success(response.getObject());
                }else {
                    callback.fail(new RuntimeException("Response error", new Throwable(response.getError())));
                }
            }
        });
    }

    static class Sync extends AbstractQueuedSynchronizer{
        private static final long serialVersionUID = 10L;

        // future status
        private final int done = 1;
        private final int preding = 0;

        @Override
        protected boolean tryAcquire(int acquires) {
            return getState() == done? true:false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == preding){
                if (compareAndSetState(preding, done)){
                    return true;
                }
            }
            return false;
        }

        public boolean isDone(){
            getState();
            return getState() == done;
        }
    }
}
