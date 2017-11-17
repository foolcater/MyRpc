package com.hua.rpc.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/11/14.
 */
public class RpcContext {
    private Map<String, Object> contextParameters;

    public Map<String, Object> getContextParameters() {
        return contextParameters;
    }

    public void setContextParameters(Map<String, Object> contextParameters) {
        this.contextParameters = contextParameters;
    }

    private static final ThreadLocal<RpcContext> rpcContextThreadLocal = new ThreadLocal<RpcContext>(){
        @Override
        protected RpcContext initialValue() {
            RpcContext context = new RpcContext();
            context.setContextParameters(new HashMap<String, Object>());
            return context;
        }
    };

    public static RpcContext getContext(){
        return rpcContextThreadLocal.get();
    }

    public static void removeContext(){
        rpcContextThreadLocal.remove();
    }

    public RpcContext() {}
}
