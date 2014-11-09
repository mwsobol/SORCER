package sorcer.arithmetic.tester.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Divider {

	@SuppressWarnings("rawtypes")
	public Context divide(Context context) throws RemoteException,
			ContextException;
}
