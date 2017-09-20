/**
 * An top-level common interface for all service peers in SORCER.
 * Each service accepts a request for {@link Mogram} to exert
 * the federation of collaborating services as specified by the mogram.
 *
 * @author Mike Sobolewski
 */
package sorcer.service;

import sorcer.service.modeling.Union;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski.
 */
public interface Activity extends Service {

    public Union act(Arg... args) throws ServiceException, RemoteException;

    public Union act(String entryName, Arg... args) throws ServiceException, RemoteException;

}
