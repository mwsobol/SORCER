package sorcer.arithmetic.tester.volume.impl;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public interface Cylinder {

	public Context getCylinderSurface(Context context) throws ContextException, RemoteException;

	public Context getCylinderVolume(Context context) throws ContextException, RemoteException;
}
