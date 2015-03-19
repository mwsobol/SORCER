package sorcer.service;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import net.jini.core.transaction.TransactionException;

public class ExertionCallable implements Callable<Exertion> {
	private Exertion exertion;

	public ExertionCallable(Exertion exertion) {
		this.exertion = exertion;
	}

	public Exertion call() throws RemoteException, TransactionException,
			ExertionException {
		if (exertion != null)
			return exertion.exert();

		return exertion;
	}

	public Exertion getExertion() {
		return exertion;
	}
}