package edu.pjatk.inn.coffeemaker;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 8/26/15.
 */
@SuppressWarnings("rawtypes")
public interface QR {

    public Context scan(Context context) throws RemoteException, ContextException;

}
