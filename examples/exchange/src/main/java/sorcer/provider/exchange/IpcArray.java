package sorcer.provider.exchange;

import sorcer.service.Closing;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Created by sobolemw on 9/21/15.
 */
public interface IpcArray extends Closing {

    public int getPort() throws RemoteException;

    public String getHostName() throws RemoteException;

    public int[] ipcIntegerArray(int[] in) throws RemoteException, IOException;

    public int[] ipcDoubleArray(int[] in) throws RemoteException, IOException;

    public void close() throws RemoteException, IOException;
}
