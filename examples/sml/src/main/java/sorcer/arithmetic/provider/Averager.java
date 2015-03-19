package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public interface Averager {
	@SuppressWarnings("rawtypes")
	public Context average(Context context)
			throws RemoteException, ContextException;
}
