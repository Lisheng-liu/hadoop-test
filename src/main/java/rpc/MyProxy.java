package rpc;

import org.apache.hadoop.ipc.ProtocolSignature;

import java.io.IOException;

public class MyProxy implements IProxyProtocol {
    @Override
    public String hello(String msg) throws IOException {
        System.out.println("invoked");
        return "hello "+msg;
    }

    @Override
    public long getProtocolVersion(String s, long l) throws IOException {
        return versionID;
    }

    @Override
    public ProtocolSignature getProtocolSignature(String s, long l, int i) throws IOException {
       return new ProtocolSignature(versionID, null);
    }
}
