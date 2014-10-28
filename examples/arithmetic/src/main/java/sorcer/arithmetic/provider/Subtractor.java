package sorcer.arithmetic.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

@SuppressWarnings("rawtypes")
public interface Subtractor {

	public Context subtract(Context context) throws RemoteException,
			ContextException;
}
