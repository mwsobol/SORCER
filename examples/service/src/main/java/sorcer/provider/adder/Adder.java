package sorcer.provider.adder;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Adder {

	String RESULT_PATH = "eval/result";

	Context add(Context context) throws RemoteException, ContextException;

	Context sum(Context context) throws RemoteException, ContextException;

	Context nothing(Context context) throws RemoteException, ContextException;
}
