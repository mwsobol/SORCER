package sorcer.core.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * Created by Mike Sobolewski on 8/30/17.
 */
public interface SessionManagement extends Remote {

    final String BEAN_SESSION = "bean/session";

    public Set getSessions() throws RemoteException;

    public Context getSession(String id) throws RemoteException;

    public Object get(String id, String key) throws RemoteException;

    public void remove(String id) throws RemoteException;

    public void clear() throws RemoteException;

}
