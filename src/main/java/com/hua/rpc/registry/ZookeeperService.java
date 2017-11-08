package com.hua.rpc.registry;

import com.hua.rpc.client.HuaConnectManager;
import com.hua.rpc.util.Utils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * zk连接管理
 * Created by Administrator on 2017/10/31.
 */
public class ZookeeperService implements Constant{
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperService.class);

    private ZooKeeper zk = null;
    private String registerAddress;
    private CountDownLatch latch = new CountDownLatch(1);

    public ZookeeperService(String registerAddress){
        LOGGER.info("开始连接zookeeper,address=" + registerAddress);
        this.registerAddress = registerAddress;
        connectZk();
        LOGGER.info("Zookeeper 连接成功");
    }

    private void connectZk(){
        try {
            zk = new ZooKeeper(registerAddress, Constant.SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (Event.KeeperState.SyncConnected == event.getState()){
                        latch.countDown();
                    }
                }
            });
            latch.await();
            LOGGER.info("connect zookeeper server successful!");
        }catch (IOException e){
            LOGGER.error("connect zookeeper server error!", e);
        }catch (InterruptedException e){
            LOGGER.error("connect zookeeper server error!", e);
        }
    }

    /**
     * 注册服务
     * @param clazzName
     * @param data
     */
    public void createProviderNode(String clazzName, String data){
        LOGGER.info("注册服务:{}, 数据为:{}", clazzName, data);
        try {
            Stat stat = zk.exists(Constant.ZK_ROOT, false);
            if (stat == null){
                zk.create(Constant.ZK_ROOT,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            stat = zk.exists(Constant.ZK_ROOT+"/"+clazzName, false);
            if (stat == null){
                zk.create(Constant.ZK_ROOT+"/"+clazzName, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            stat = zk.exists(Constant.ZK_ROOT+"/"+clazzName+Constant.ZK_PROVIDER, false);
            if (stat == null){
                zk.create(Constant.ZK_ROOT+"/"+clazzName+Constant.ZK_PROVIDER, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String path = zk.create(Constant.ZK_ROOT+"/"+clazzName+Constant.ZK_PROVIDER+Constant.ZK_PREFIX, data.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.info("create zookeeper node ({} => {})", path, data);
        }catch (KeeperException e){
            LOGGER.error("create provider {} error.",clazzName);
        }catch (InterruptedException e){
            LOGGER.error("create provider {} error.",clazzName);
        }
    }

    /**
     * 注册服务
     * @param classNames
     * @param data
     */
    public void createProviderNode(List<String> classNames, String data){
        if (classNames != null && !classNames.isEmpty()){
            for (String className : classNames)
                createProviderNode(className, data);
        }
    }

    /**
     * 发现服务
     * @return
     */
    public void findService(String className){
        String path = Constant.ZK_ROOT+"/"+className;
        try {
            Stat stat = zk.exists(path, false);
            if (stat == null) {
                LOGGER.info("节点" + path + "不存在");
                return;
            }
            path = path+Constant.ZK_PROVIDER;
            List<String> list = zk.getChildren(path, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    LOGGER.error("change type:{}", event);
                    if (event.getType() == Event.EventType.NodeChildrenChanged){
                        LOGGER.info("children has changed");
                        findService(className);
                    }
                }
            });
            LOGGER.info("服务:{},获取服务列表:{}", path, list);
            createConsumerNode(className);
            if (list != null && !list.isEmpty()){
                List<String> result = new ArrayList<String>(list.size());
                for (String node : list){
                    byte[] data =  zk.getData(path+"/"+node, false, null);
                    String serverNode = new String(data);
                    LOGGER.info("节点：{}，数据为:{}", path+"/"+node, serverNode);
                    result.add(serverNode);
                }
                HuaConnectManager.getInstance().updateServiceHost(className, result);
            }
        }catch (KeeperException e){
            LOGGER.error("获取服务出现异常", e);
        }catch (InterruptedException e){
            LOGGER.error("获取服务出现异常", e);
        }
    }

    /**
     * 创建消费者
     * @param className
     */
    private void createConsumerNode(String className){
        try {
            String path = Constant.ZK_ROOT+"/"+className+Constant.ZK_CONSUMER;
            Stat stat = zk.exists(path, false);
            if (stat == null){
                String createPath = zk.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                LOGGER.info("create node {} successful", createPath);
            }
            InetAddress address = Utils.getLocalHostLANAddress();
            boolean needCreate = true;
            List<String> childrens = zk.getChildren(path, false);
            if (childrens == null || childrens.isEmpty()){
                String createPath = zk.create(path+Constant.ZK_PREFIX, address.getHostAddress().getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                LOGGER.info("successful create node {} , data:{}", createPath, address.getAddress());
                return;
            }
            for (String node : childrens){
                byte[] data = zk.getData(path+"/"+node, false, null);
                if (address.getHostAddress().equals(new String(data))){
                    needCreate = false;
                }
            }
            if (needCreate){
                String createPath = zk.create(path+Constant.ZK_PREFIX, address.getHostAddress().getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                LOGGER.info("successful create node {} , data:{}", createPath, address.getAddress());
            }
        }catch (InterruptedException e){
            LOGGER.info("create consumer node error, class = " + className, e);
        }catch (KeeperException e){
            LOGGER.info("create consumer node error, class = " + className, e);
        }
    }

    public void stop(){
        if (zk != null){
            try {
                zk.close();
            }catch (InterruptedException e){
                LOGGER.error("关闭zk客户端失败", e);
            }
        }
    }
}
