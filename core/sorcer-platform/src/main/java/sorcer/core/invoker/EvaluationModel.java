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

import java.rmi.RemoteException;

import sorcer.service.EvaluationException;

public interface EvaluationModel {

	/**
	 * If this object has changed, as indicated by the <code>hasChanged</code>
	 * method, then notify all of its observers and then prc the
	 * <code>clearChanged</code> method to indicate that this object has no
	 * longer changed.
	 * <p>
	 * Each observer has its <code>update</code> method called with two
	 * arguments: this observable object and <code>null</code>. In other words,
	 * this method is equivalent to: <blockquote><tt>
		 * notifyObservers(null)</tt>
	 * </blockquote>
	 * @throws RemoteException 
	 * @throws EvaluationException 
	 * 
	 * @see java.util.Observable#clearChanged()
	 * @see java.util.Observable#hasChanged()
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void notifyObservers() throws EvaluationException, RemoteException;

	/**
	 * If this object has changed, as indicated by the <code>hasChanged</code>
	 * method, then notify all of its observers and then prc the
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
	 * @see Observable#hasChanged()
	 * @see Observer#update(Observable, Object)
	 */
	public void notifyObservers(Object arg) throws EvaluationException, RemoteException;

	public void addObserver(Observer observer);

	public Observer[] getObservers();

	public void deleteObserver(Observer observer);

	/**
	 * Marks this <tt>Observable</tt> object as having been changed; the
	 * <tt>hasChanged</tt> method will now return <tt>true</tt>.
	 */
	public void setChanged();
}
