package com.hua.rpc.registry;

import com.hua.rpc.client.ConnectManage;
import io.netty.util.internal.ThreadLocalRandom;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 服务发现
 */
public class ServiceDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<String>();

    private String registerAddress;
    private ZooKeeper zooKeeper;

    public ServiceDiscovery(String registerAddress) {
        this.registerAddress = registerAddress;
        zooKeeper = connectServer();
        if (zooKeeper != null){
            watchNode(zooKeeper);
        }
    }

    public String disvover(){
        String data = null;
        int size = dataList.size();
        if (size > 0){
            if (size == 1){
                data = dataList.get(0);
                LOGGER.debug("using only data: {}", data);
            }else {
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.info("using random data: {}", data);
            }
        }
        return data;
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
        }catch (IOException e){
            LOGGER.error("", e);
        }catch (InterruptedException e){
            LOGGER.error("", e);
        }
        return zk;
    }

    private void watchNode(final ZooKeeper zk){
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTER_PATH, new Watcher() {
                public void process(WatchedEvent event) {
                    if (event.getType()  == Event.EventType.NodeChildrenChanged){
                        LOGGER.info("child has changed");
                        watchNode(zk);
                    }
                }
            });
            List<String> dataList = new ArrayList<String>();
            for (String node : nodeList){
                byte[] bytes = zk.getData(Constant.ZK_REGISTER_PATH+"/"+node, false, null);
                dataList.add(new String(bytes));
            }
            LOGGER.debug("node data: {}", dataList);
            this.dataList = dataList;

            LOGGER.debug("service discovery triggered updating connected server node");
            UpdateConnectedServer();
        }catch (KeeperException e){
            LOGGER.error("", e);
        }catch (InterruptedException e){
            LOGGER.error("",e);
        }
    }

    private void UpdateConnectedServer(){
        LOGGER.info("更新连接：{}", this.dataList);
        ConnectManage.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop(){
        if (zooKeeper != null){
            try {
                zooKeeper.close();
            }catch (InterruptedException e){
                LOGGER.error("", e);
            }
        }
    }
}
