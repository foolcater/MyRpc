package com.hua.rpc.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 服务注册
 */
public class ServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private String registerAddress;

    public ServiceRegistry(String registerAddress) {
        this.registerAddress = registerAddress;
    }

    public void register(String data){
        if (data != null){
            ZooKeeper zk =connectServer();
            if (zk != null){
                addRootNode(zk);
                createNode(zk, data);
            }
        }
    }

    private ZooKeeper connectServer(){
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registerAddress, Constant.ZK_TIMEOUT, new Watcher() {
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected){
                        latch.countDown();
                    }
                }
            });
            latch.await();
            LOGGER.info("zk 连接成功");
        }catch (IOException e){
            LOGGER.error("", e);
        }catch (InterruptedException e){
            LOGGER.error("", e);
        }
        return zk;
    }

    private void addRootNode(ZooKeeper zk){
        try {
            Stat s = zk.exists(Constant.ZK_REGISTER_PATH, false);
            if (s == null){
                zk.create(Constant.ZK_REGISTER_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }catch (KeeperException e){
            LOGGER.error("", e);
        }catch (InterruptedException e){
            LOGGER.error("", e);
        }
    }

    private void createNode(ZooKeeper zk, String data){
        try {
            byte[] bytes = data.getBytes();
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.debug("create zookeeper node ({} => {})", path, data);
        }catch (KeeperException e){

        }catch (InterruptedException e){

        }
    }
}
