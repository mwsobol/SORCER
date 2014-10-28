package junit.sorcer.core.invoker.service;

import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public interface Sphere {

	public Context getSphereSurface(Context context) throws ContextException;

	public Context getSphereVolume(Context context) throws ContextException;
}
