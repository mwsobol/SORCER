/**
 * An top-level common interface for all service peers in SORCER.
 * Each service accepts a request for {@link Mogram} to exert
 * the federation of collaborating services as specified by the mogram.
 *
 * @author Mike Sobolewski
 */
package sorcer.service;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski.
 */
public interface Service {

    public  <T extends Servicer> Object exec(T srv, Arg... entries) throws MogramException, RemoteException;

}
