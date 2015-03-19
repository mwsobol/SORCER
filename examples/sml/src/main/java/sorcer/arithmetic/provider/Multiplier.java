package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Multiplier {

	public Context multiply(Context context) throws RemoteException,
			ContextException;
}
