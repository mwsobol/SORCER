/**
 * An top-level common interface for all service peers in SORCER.
 * Each service accepts a request for {@link Mogram} to exert
 * the federation of collaborating services as specified by the mogram.
 *
 * @author Mike Sobolewski
 */
package sorcer.service;

import net.jini.core.transaction.TransactionException;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski.
 */
public interface Service {

    public Object execute(Arg... args) throws ServiceException, RemoteException;

}
