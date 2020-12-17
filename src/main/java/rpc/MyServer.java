package rpc;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;


import java.io.IOException;

public class MyServer {

    private static final String HOST = "localhost";
    private static final int PORT = 2181;

    public static void main(String[] args) throws IOException {
        Configuration configuration = new Configuration();
        Server server = new RPC.Builder(configuration).setProtocol(IProxyProtocol.class).
                setInstance(new MyProxy()).setBindAddress(HOST).
                setNumHandlers(2).
                setPort(PORT).build();
        server.start();
    }
}


