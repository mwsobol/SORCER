package sorcer.arithmetic.tester.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public interface Adder {

	@SuppressWarnings("rawtypes")
	public Context add(Context context) throws RemoteException,
			ContextException;
}
