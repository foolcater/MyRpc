package com.hua.rpc.registry;

/**
 * 常量类
 */
public interface Constant {

    int ZK_TIMEOUT = 5000;

    //节点名称 /MyRpc/com.hua.rpc.server.HelloService/provider/node-0000000001

    String ZK_ROOT = "/MyRpc";

    String ZK_PROVIDER = "/provider";

    String ZK_CONSUMER = "/consumer";

    String ZK_PREFIX = "/node-";

    String ZK_REGISTER_PATH = "/register";

    String ZK_DATA_PATH= ZK_REGISTER_PATH + "/data";

    Integer SESSION_TIMEOUT = 5000;
}
