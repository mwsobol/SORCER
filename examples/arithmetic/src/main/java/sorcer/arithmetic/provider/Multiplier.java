package sorcer.arithmetic.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

@SuppressWarnings("rawtypes")
public interface Multiplier {

	public Context multiply(Context context) throws RemoteException,
			ContextException;
}
