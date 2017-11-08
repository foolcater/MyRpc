package com.hua.rpc.service.impl;

import com.hua.rpc.annotation.RpcService;
import com.hua.rpc.service.HahaService;

/**
 * Created by Administrator on 2017/11/3.
 */
@RpcService(HahaService.class)
public class HahaServiceImpl implements HahaService{

    @Override
    public String haha(String name) {
        return "i am huazai you name is " + name;
    }
}
