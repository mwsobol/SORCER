package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Subtractor {

	public Context subtract(Context context) throws RemoteException,
			ContextException;
}
