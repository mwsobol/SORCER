package junit.sorcer.core.invoker.service;

import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public interface Cylinder {

	public Context getCylinderSurface(Context context) throws ContextException;

	public Context getCylinderVolume(Context context) throws ContextException;
}
