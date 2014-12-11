package sorcer.worker.provider;

import java.rmi.Remote;
import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings("rawtypes")
public interface Worker extends Remote {

	Context sayHi(Context context) throws RemoteException, ContextException;

	Context sayBye(Context context) throws RemoteException, ContextException;

	Context doWork(Context context) throws InvalidWork, RemoteException,
			ContextException;
}
