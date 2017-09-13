package sorcer.core.provider;

import net.jini.id.Uuid;
import sorcer.service.Context;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Mike Sobolewski on 8/30/17.
 */
public interface SessionManagement extends Remote {

    final String BEAN_SESSION = "bean/session";

    public List<Uuid> getSessionIds() throws RemoteException;

    public Context getSession(Uuid id) throws RemoteException;

    public void removeSession(String id) throws RemoteException;

    public void clearSessions() throws RemoteException;

}
