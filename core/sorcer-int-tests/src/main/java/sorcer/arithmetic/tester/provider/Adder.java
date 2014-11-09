package sorcer.arithmetic.tester.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Adder {

	@SuppressWarnings("rawtypes")
	public Context add(Context context) throws RemoteException,
			ContextException;
}
