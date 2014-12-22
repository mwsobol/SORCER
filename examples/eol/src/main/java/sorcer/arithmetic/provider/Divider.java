package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Divider {

	public Context divide(Context context) throws RemoteException,
			ContextException;
}
