package sorcer.provider.adder;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Adder {

	public Context add(Context context) throws RemoteException, ContextException;

    public Context sum(Context context) throws RemoteException, ContextException;
}
