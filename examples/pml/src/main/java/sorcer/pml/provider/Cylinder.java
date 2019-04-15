package sorcer.pml.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public interface Cylinder {

	public Context getCylinderSurface(Context context) throws ContextException, RemoteException;

	public Context getCylinderVolume(Context context) throws ContextException, RemoteException;
}
