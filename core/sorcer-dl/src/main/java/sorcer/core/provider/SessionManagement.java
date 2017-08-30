package sorcer.core.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.Remote;

/**
 * Created by Mike Sobolewski on 8/30/17.
 */
public interface SessionManagement extends Remote {

    public Context getSession(String id) throws RuntimeException;

    public Object get(String id, String key) throws RuntimeException;

    public void remove(String id) throws RuntimeException;

    public void clear() throws RuntimeException;

}
