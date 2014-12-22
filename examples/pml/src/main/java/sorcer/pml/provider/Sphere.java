package sorcer.pml.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public interface Sphere {

	public Context getSphereSurface(Context context) throws ContextException, RemoteException;

	public Context getSphereVolume(Context context) throws ContextException, RemoteException;
}
