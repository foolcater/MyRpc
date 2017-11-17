package com.hua.rpc.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by Administrator on 2017/11/1.
 */
public class Utils {

    public static InetAddress getLocalHostLANAddress(){
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ){
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ){
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()){
                        if (inetAddr.isSiteLocalAddress()){
                            return inetAddr;
                        }else if (candidateAddress == null){
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress !=null)
                return candidateAddress;
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            return jdkSuppliedAddress;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
