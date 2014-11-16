package sorcer.arithmetic.tester.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public interface Multiplier {

	@SuppressWarnings("rawtypes")
	public Context multiply(Context context) throws RemoteException,
			ContextException;
}
