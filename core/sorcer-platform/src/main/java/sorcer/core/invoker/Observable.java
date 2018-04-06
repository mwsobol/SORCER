/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.core.invoker;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jini.id.Uuid;
import sorcer.service.EvaluationException;
import sorcer.service.Identifiable;

public class Observable implements EvaluationModel, Serializable {
	static final long serialVersionUID = -6036250788560831439L;
	protected boolean changed = false;
	protected static Logger logger = LoggerFactory.getLogger(Observable.class);
	// initialized in addObserver
	// observer id-observer self
	protected Map<Uuid, Observer> observerMap = null;

	/**
	 * Construct an Observable with no Observers. Initialize observerMap when
	 * the first one is added.
	 */
	public Observable() {
		// do nothing
	}

	public boolean hasObservers() {
		if (countObservers() > 0)
			return true;
		return false;
	}

	public int countObservers() {
		if (observerMap == null)
			return 0;
		return observerMap.size();
	}

	public void clearObservers() {
		observerMap.clear();	
	}
	
	/**
	 * If this object has changed, as indicated by the <code>isChanged</code>
	 * method, then notify all of its observers and then call the
	 * <code>clearChanged</code> method to indicate that this object has no
	 * longer changed.
	 * <p>
	 * Each observer has its <code>update</code> method called with two
	 * arguments: this observable object and <code>null</code>. In other
	 * words, this method is equivalent to: <blockquote><tt>
	 * notifyObservers(null)</tt></blockquote>
	 * @throws RemoteException 
	 * @throws EvaluationException 
	 * 
	 * @see java.util.Observable#clearChanged()
	 * @see java.util.Observable#hasChanged()
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void notifyObservers() throws EvaluationException, RemoteException {
		notifyObservers(null);
	}

	/**
	 * If this object has changed, as indicated by the <code>isChanged</code>
	 * method, then notify all of its observers and then call the
	 * <code>clearChanged</code> method to indicate that this object has no
	 * longer changed.
	 * <p>
	 * Each observer has its <code>update</code> method called with two
	 * arguments: this observable object and the <code>arg</code> argument.
	 * 
	 * @param arg
	 *            any object.
	 * @throws RemoteException 
	 * @throws EvaluationException 
	 * @see Observable#clearChanged()
	 * @see Observable#isChanged()
	 * @see Observer#update(Observable, Object)
	 */
	public void notifyObservers(Object arg) throws EvaluationException, RemoteException {
		/*
		 * a temporary array buffer, used as a snapshot of the state of current
		 * Observers.
		 */
		Object[] arrLocal = null;
		synchronized (this) {
			/*
			 * We don't want the Observer doing callbacks into arbitrary code
			 * while holding its own Monitor. The code where we extract each
			 * Observable from the Vector and store the state of the Observer
			 * needs synchronization, but notifying observers does not (should
			 * not). The worst result of any potential race-condition here is
			 * that: 1) a newly-added Observer will miss a notification in
			 * progress 2) a recently unregistered Observer will be wrongly
			 * notified when it doesn't care
			 */
			if (!changed)
				return;
			if (observerMap != null) {
				arrLocal = observerMap.values().toArray();
			}
			clearChanged();
		}
		if (arrLocal != null)
			for (int i = arrLocal.length - 1; i >= 0; i--) {
//				logger.info("Observable.notifyObservers(): I am observable \"" 
//						+ toString() + "\", \ncalling arrLocal[i].update(this, arg) \n" 
//						+ "with the following args: \n"
//						+ "i = " + i + "\narrLocal[i] = " + arrLocal[i]
//						+ "\narrLocal[i].getClass() = " + arrLocal[i].getClass()                                             
//						+ "\nthis = " + this + "\narg = " + arg + "\n");
				((Observer) arrLocal[i]).update(this, arg);
//				logger.info("Observable.notifyObservers(): done calling updates");
			}
		//notifyParentObservers(arg);
	}

	public void addObserver(Observer observer) {
		if (observerMap == null)
			observerMap = new HashMap<Uuid, Observer>();
		observerMap.put((Uuid)((Identifiable)observer).getId(), observer);
	}

	public Observer[] getObservers() {
		if (observerMap == null)
			return null;
		Observer[] obs = new Observer[observerMap.size()];
		observerMap.values().toArray(obs);
		return obs;
	}

	public void deleteObserver(Observer observer) {
		if (observerMap != null)
			observerMap.remove(((Identifiable)observer).getId());
	}

	/**
	 * Marks this <tt>Observable</tt> object as having been changed; the
	 * <tt>isChanged</tt> method will now return <tt>true</tt>.
	 */
	public synchronized void setChanged() {
		changed = true;
	}

	public boolean isChanged() {
		return changed;
	}

	/**
	 * Indicates that this object has no longer changed, or that it has already
	 * notified all of its observers of its most recent change, so that the
	 * <tt>hasChanged</tt> method will now return <tt>false</tt>. This
	 * method is called automatically by the <code>notifyObservers</code>
	 * methods.
	 * 
	 * @see java.util.Observable#notifyObservers()
	 * @see java.util.Observable#notifyObservers(java.lang.Object)
	 */
	protected synchronized void clearChanged() {
		changed = false;
	}
	
	public void notifyParentObservers(Object obj) throws EvaluationException,
			RemoteException {
		if (observerMap == null)
			return;
		Iterator<Observer> i = observerMap.values().iterator();
		while (i.hasNext()) {
			EvaluationModel o = (EvaluationModel) i.next();
			logger.info("Observable.notifyParentObservers(): o = " + o);

			o.setChanged();
			o.notifyObservers(obj);
		}
		logger.info("Observable.notifyParentObservers(): done notifying parents");
	}
	
	public Map<Uuid, Observer> getObserverMap() {
		return observerMap;
	}
	
}
