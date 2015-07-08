package sorcer.arithmetic.tester.volume.impl;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public interface Sphere {

	public Context getSphereSurface(Context context) throws ContextException, RemoteException;

	public Context getSphereVolume(Context context) throws ContextException, RemoteException;
}
