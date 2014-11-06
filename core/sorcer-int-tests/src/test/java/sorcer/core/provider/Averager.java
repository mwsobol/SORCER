package junit.sorcer.core.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Averager {
	public Context average(Context context)
			throws RemoteException, ContextException;
}
