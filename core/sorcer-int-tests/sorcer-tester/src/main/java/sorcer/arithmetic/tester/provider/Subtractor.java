package sorcer.arithmetic.tester.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Subtractor {

	@SuppressWarnings("rawtypes")
	public Context subtract(Context context) throws RemoteException,
			ContextException;
}
