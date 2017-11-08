package com.hua.rpc.client.proxy;

import com.hua.rpc.client.ConnectManage;
import com.hua.rpc.client.HuaConnectManager;
import com.hua.rpc.client.RPCFuture;
import com.hua.rpc.client.RpcClientHandler;
import com.hua.rpc.protocol.RpcRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

public class ObjectProxy<T> implements InvocationHandler, IAsyncObjectProxy{
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectProxy.class);
    private Class<T> clazz;

    public ObjectProxy(Class<T> clazz) {
        this.clazz = clazz;
    }


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()){
            String name = method.getName();
            if("equals".equals(name)){
                return proxy == args[0];
            }else if ("hashCode".equals(name)){
                return System.identityHashCode(proxy);
            }else if ("toString".equals(name)){
                return proxy.getClass().getName()+"@"+
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            }else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameters(args);
        request.setParameterTypes(method.getParameterTypes());
        // DEBUG
        LOGGER.debug(method.getDeclaringClass().getName());
        LOGGER.debug(method.getName());
        for (int i=0; i<method.getParameterTypes().length; i++){
            LOGGER.debug(method.getParameterTypes()[i].getName());
        }
        for (int i=0; i<args.length; i++){
            LOGGER.debug(args[i].toString());
        }
        RpcClientHandler handler = HuaConnectManager.getInstance().getConnection(request.getClassName());
        LOGGER.info("获取连接成功,发送参数:{}",request);
        LOGGER.info("请求服务器地址:{}", handler.getRemotePeer());
        RPCFuture rpcFuture = handler.sendRequest(request);
        LOGGER.info("发送数据请求成功,等待结果返回");
        return rpcFuture.get();
    }

    public RPCFuture call(String funcName, Object... args) {
        RpcClientHandler handler = ConnectManage.getInstance().chooseHandler();
        RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
        RPCFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args){
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);

        Class[] parameterTypes = new Class[args.length];

        for (int i=0; i<args.length; i++){
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);

        return request;
    }

    private Class<?> getClassType(Object obj){
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName){
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case  "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }
        return classType;
    }

}
