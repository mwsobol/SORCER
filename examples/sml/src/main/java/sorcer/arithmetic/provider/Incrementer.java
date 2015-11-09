package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Incrementer {

	public Context increment(Context context) throws RemoteException,
			ContextException;
}
