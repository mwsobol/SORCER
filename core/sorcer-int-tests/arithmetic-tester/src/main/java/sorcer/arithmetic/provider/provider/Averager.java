package sorcer.arithmetic.tester.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Averager {
	@SuppressWarnings("rawtypes")
	public Context average(Context context)
			throws RemoteException, ContextException;
}
