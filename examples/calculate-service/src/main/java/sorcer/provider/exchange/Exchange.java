package sorcer.provider.exchange;

import java.rmi.RemoteException;
import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * Created by sobolemw on 8/5/15.
 */
public interface Exchange {

    public Context exchange(Context context) throws RemoteException, ContextException;

    public int[] exchange(int[] input) throws RemoteException;

}
