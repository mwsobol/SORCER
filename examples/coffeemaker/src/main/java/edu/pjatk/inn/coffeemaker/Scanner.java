package edu.pjatk.inn.coffeemaker;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 8/26/15.
 */
@SuppressWarnings("rawtypes")
public interface Scanner {
    String latteCode = "c1,m1,s2,ch0,p123,Latte";
    String cappuccinoCode = "c2,m1,s1,ch0,p32,Cappuccino";
    String chocoCode = "c0,m1,s2,ch1,p23,Choco";

    public Context scan(Context context) throws RemoteException, ContextException;

}
