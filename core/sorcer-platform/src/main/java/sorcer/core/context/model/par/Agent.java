/**
 * 
 */
package sorcer.core.context.model.par;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.MethodInvoker;
import sorcer.service.Arg;
import sorcer.service.Context;
import sorcer.service.EvaluationException;

/**
 * @author Mike Sobolewski
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Agent<T> extends ParEntry<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private URL[] agentURLs;
	
	private String className;
	
	private MethodInvoker invoker;
	
	transient private URLClassLoader agentLoader;

	public Agent(String name, URL... agentURLs) {
		super(name);
		this.agentURLs = agentURLs;
	}
	
	public Agent(String name, String className, URL... agentURLs)
			throws EvaluationException, RemoteException {
		super(name);
		this.className = className;
		this.agentURLs = agentURLs;
	}
	
	public T evaluate(Arg... entries)
			throws EvaluationException, RemoteException {
		if (invoker != null)
			return (T)invoker.invoke(getPars(entries));
					
		if (className == null)
			className = getClassName(entries);

		ClassLoader cl = getClass().getClassLoader();
		try {
			agentLoader = URLClassLoader.newInstance(agentURLs, cl);
			final Thread currentThread = Thread.currentThread();
			final ClassLoader parentLoader = (ClassLoader) AccessController
					.doPrivileged(new PrivilegedAction() {
						public Object run() {
							return (currentThread.getContextClassLoader());
						}
					});

			try {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						currentThread.setContextClassLoader(agentLoader);
						return (null);
					}
				});
				Class clazz = null;
				try {
					clazz = agentLoader.loadClass(className);
				} catch (ClassNotFoundException ex) {
					ex.printStackTrace();
					throw ex;
				}
				Constructor constructor = clazz
						.getConstructor(new Class[] { Context.class });
				
				Object obj = constructor
						.newInstance(new Object[] { scope });
				invoker = new MethodInvoker(name, obj, name, getPars(entries));
				if (scope instanceof ParModel)
					invoker.setScope((ParModel)scope);
				invoker.setContext(scope);
			} finally {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						currentThread.setContextClassLoader(parentLoader);
						return (null);
					}
				});
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					"Unable to instantiate par agent :"
							+ e.getClass().getName() + ": "
							+ e.getLocalizedMessage());
		}
		value = (T)invoker.invoke(entries);
		invoker.valueValid(true);
		return value;
	}
	
	@Override
	public T getValue(Arg... entries) throws EvaluationException, RemoteException {
		if (value != null && invoker != null && invoker.valueValid())
			return value;
		else
			return (T)evaluate(entries);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getAsis()
	 */
	@Override
	public T asis() throws EvaluationException, RemoteException {
		return (T) invoker;
	}
	
	private ParEntry[] getPars(Arg... entries) {
		ParEntry[] pa = new ParEntry[entries.length];
		if (entries != null && entries.length > 0) {
			for (int i = 0; i < entries.length; i++)
				pa[i] = (ParEntry) entries[i];
		}
		return pa;
	}
	
	private String getClassName(Arg... entries) {
		for (Arg p : entries) {
			if (p instanceof Entry && ((Entry)p).key().equals("class"))
				return (String)((Entry)p).value();
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "agent [" + name + ":" + Arrays.toString(agentURLs) + "]";
	}
}
