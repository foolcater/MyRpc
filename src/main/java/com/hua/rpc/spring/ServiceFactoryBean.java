package com.hua.rpc.spring;

import com.hua.rpc.client.RpcClient;
import org.springframework.beans.factory.FactoryBean;


public class ServiceFactoryBean implements FactoryBean{

    private Class<?> interfaceClass;


    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    public Object getObject() throws Exception {
        return RpcClient.create(interfaceClass);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
