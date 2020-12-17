package rpc;

import org.apache.hadoop.ipc.VersionedProtocol;

import java.io.IOException;

public interface IProxyProtocol extends VersionedProtocol {
    //版本号，默认情况下，不同版本号的RPC客户端与Server之间不能相互通信
    public static final long versionID = 1L;
    public String hello(String msg) throws IOException;

}
