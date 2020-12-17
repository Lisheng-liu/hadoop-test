package rpc;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyClient {
    private static final String HOST = "localhost";
    private static final int PORT = 2181;

    public static void main(String[] args) throws IOException {
        Configuration configuration = new Configuration();
        IProxyProtocol proxy = RPC.getProxy(IProxyProtocol.class,IProxyProtocol.versionID,new InetSocketAddress(HOST,PORT),configuration);
        String result = proxy.hello("world");
        System.out.println(result);
    }
}
