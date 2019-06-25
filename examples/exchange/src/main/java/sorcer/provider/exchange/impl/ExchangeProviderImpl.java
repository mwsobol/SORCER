/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United
 * States Air Force Research Lab. Any further distribution or use by anyone or
 * any data contained therein, unless otherwise specifically provided for, is
 * prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street
 * Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of
 * the United States Government. Neither the United States Government nor the
 * United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the
 * accuracy, completeness, or usefulness of any information, apparatus,
 * product, or process disclosed, or represents that its use would not infringe
 * privately owned rights.
 */

package sorcer.provider.exchange.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.ServiceExerter;
import sorcer.service.Exerter;
import sorcer.provider.exchange.ExchangeRemote;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;

public class ExchangeProviderImpl implements ExchangeRemote, Serializable {

    private static Logger logger = LoggerFactory.getLogger(ExchangeProviderImpl.class.getName());

    private ServiceExerter provider;

    private int BB_SIZE = 1024;

    private ServerSocketChannel serverChannel;

    public ExchangeProviderImpl() throws RemoteException {
        // empty
    }

    public void init(Exerter provider) {
        this.provider = (ServiceExerter) provider;


        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress("127.0.0.1", 9001));

            Runnable ipc = new Runnable() {
                @Override
                public void run() {
                    listen();
                }
            };

            new Thread(ipc).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Context exchangeInts(Context context) throws RemoteException, ContextException {
            int[] values = (int[]) context.getValue("values");
            for (int n = 0; n < values.length; n++) {
                values[n] += 1;
            }
            context.putValue("values", values);
            return context;
    }

    @Override
    public Context exchangeDoubles(Context context) throws RemoteException, ContextException {
            double[] values = (double[]) context.getValue("values");
            for (int n = 0; n < values.length; n++) {
                values[n] += 1.0;
            }
            context.putValue("values", values);
            return context;
    }

    @Override
    public int[] exchangeInts(int[] input) throws RemoteException {
        for (int n = 0; n < input.length; n++) {
            input[n] += 1;
        }
        return input;
    }

    @Override
    public double[] exchangeDoubles(double[] input) throws RemoteException {
        for (int n = 0; n < input.length; n++) {
            input[n] += 1.0;
        }
        return input;
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

    private int[] processDataBuffer(IntBuffer buffer) {
        int resultSize = buffer.capacity();
        int[] result = new int[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = buffer.get(i) + 1;
        }
        return result;
    }
}
