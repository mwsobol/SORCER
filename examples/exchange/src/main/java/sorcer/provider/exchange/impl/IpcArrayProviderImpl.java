package sorcer.provider.exchange.impl;

import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;
import sorcer.core.provider.ServiceTasker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;

/**
 * Created by sobolemw on 9/21/15.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class IpcArrayProviderImpl extends ServiceTasker {

    private int BB_SIZE = 1024;

    private ServerSocketChannel serverChannel;

    private String hostName;

    private int port;

    public IpcArrayProviderImpl() throws RemoteException {

    }

    public IpcArrayProviderImpl(String[] args, LifeCycle lifeCycle)
            throws Exception {
        super(args, lifeCycle);
        init();
    }

    @Override
    protected void providerSetup() {
        port = 9010;
        hostName = delegate.getHostName();
        IpcArrayBean smartProxy = new IpcArrayBean();
        smartProxy.setPort(port);
        smartProxy.setHostName(hostName);
        delegate.setSmartProxy(smartProxy);
    }

    public int getPort() {
        return port;
    }

    public String getHostName() {
        return hostName;
    }

    public void init() throws ConfigurationException {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(hostName, port));

            Runnable ipc = new Runnable() {
                @Override
                public void run() {
                    listen();
                }
            };

            new Thread(ipc).start();
            listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
        delegate.init(this);
    }

    private int[] processDataBuffer(IntBuffer buffer) {
        int resultSize = buffer.capacity();
        int[] result = new int[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = buffer.get(i) + 1;
        }
        return result;
    }

    private void listen() {
        try {
            ByteBuffer bb = ByteBuffer.allocate(BB_SIZE);
            IntBuffer ib = bb.asIntBuffer();
            SocketChannel client;
            while (true) {
                client = serverChannel.accept();
                if (client != null) {
                    bb.clear();
                    client.read(bb);

                    // processing the read buffer
                    int[] result  = processDataBuffer(ib);

                    ib.clear();
                    ib.put(result);
//                    logger.info("result array: " + Arrays.toString(result));
                    bb.clear();
                    while (bb.hasRemaining()) {
                        int written = client.write(bb);
//                        logger.info("bytes written: " + written);
                    }
                    client.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverChannel != null)
                try {
                    serverChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

}
