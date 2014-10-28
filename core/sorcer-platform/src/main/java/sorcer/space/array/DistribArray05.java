/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.space.array;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.entry.UnusableEntriesException;
import net.jini.space.JavaSpace05;

public class DistribArray05 {
	private JavaSpace05 space;

	private String name;

	public DistribArray05(JavaSpace05 space, String name) {
		this.space = space;
		this.name = name;
	}

	public void create() throws RemoteException, TransactionException,
			UnusableEntryException, InterruptedException {
		Start start = new Start();
		start.name = name;
		start.position = new Integer(0);

		End end = new End();
		end.name = name;
		end.position = new Integer(0);

		Start startTemplate = new Start();
		startTemplate.name = name;

		End endTemplate = new End();
		endTemplate.name = name;

		Start starte = (Start) space.readIfExists(startTemplate, null,
				Long.MAX_VALUE);
		End ende = (End) space.readIfExists(endTemplate, null, Long.MAX_VALUE);

		if ((starte == null) || (ende == null)) {
			space.write(start, null, Lease.FOREVER);
			space.write(end, null, Lease.FOREVER);
		}
	}

	public int append(Object obj, Transaction txn) throws RemoteException,
			TransactionException, UnusableEntryException, InterruptedException {
		End template = new End();
		template.name = name;

		End end = (End) space.take(template, txn, 250);
		if (end != null) {
			int position = end.increment();
			space.write(end, txn, Lease.FOREVER);

			Element element = new Element(name, position, obj);
			space.write(element, txn, Lease.FOREVER);
			return position;
		}
		return -1;
	}

	public int append(Object obj) throws RemoteException, TransactionException,
			UnusableEntryException, InterruptedException {
		return append(obj, null);
	}

	public int size(Transaction txn) throws RemoteException,
			TransactionException, UnusableEntryException, InterruptedException {
		Start startTemplate = new Start();
		startTemplate.name = name;

		End endTemplate = new End();
		endTemplate.name = name;

		Start start = (Start) space.read(startTemplate, txn, Long.MAX_VALUE);
		End end = (End) space.read(endTemplate, txn, Long.MAX_VALUE);

		return (end.position.intValue() - start.position.intValue());
	}

	public Object readElement(int pos) throws RemoteException,
			TransactionException, UnusableEntryException, InterruptedException {
		Element template = new Element(name, pos, null);

		Element element = (Element) space.read(template, null, Long.MAX_VALUE);
		return element.data;
	}

	public Object readElementbyData(Object data) throws RemoteException,
			TransactionException, UnusableEntryException, InterruptedException {
		Element template = new Element(name, data);

		Element element = (Element) space.read(template, null, Long.MAX_VALUE);
		return element.data;
	}

	public Object takeElement(int pos)
			throws RemoteException, TransactionException, InterruptedException,
			UnusableEntryException {
		Element element = new Element(name, pos, null);
		return space.take(element, null, Long.MAX_VALUE);
	}
	
	public Object takeElement(int pos, Transaction txn, long timeout)
			throws RemoteException, TransactionException, InterruptedException, UnusableEntryException {
		Element element = new Element(name, pos, null);
		return space.take(element, txn, timeout);
	}
	
	public Collection takeElements(Transaction txn, long timeout, int maxEntries)
			throws RemoteException, TransactionException,
			UnusableEntriesException, InterruptedException {
		Element element = new Element(name, null);
		Collection tmpls = new ArrayList();
		tmpls.add(element);
		return space.take(tmpls, txn, timeout, maxEntries);
	}

	public boolean delete(Transaction txn) throws RemoteException,
			TransactionException, UnusableEntryException,
			UnusableEntriesException, InterruptedException {
		return delete(txn, false);
	}

	public boolean delete(Transaction txn, boolean force)
			throws RemoteException, TransactionException,
			UnusableEntryException, UnusableEntriesException,
			InterruptedException {
		if ((this.size(txn) > 0) && (!force))
			return false;
		else {
			this.takeElements(txn, 500, Integer.MAX_VALUE);
			Start startTemplate = new Start();
			startTemplate.name = name;

			End endTemplate = new End();
			endTemplate.name = name;

			Start starte = (Start) space.takeIfExists(startTemplate, txn,
					Long.MAX_VALUE);
			End ende = (End) space.takeIfExists(endTemplate, txn,
					Long.MAX_VALUE);
			return true;
		}
	}
	
	/**
	 * <p>
	 * Returns the JavaSpace used by this distributed array.
	 * </p>
	 * 
	 * @return the JavaSpace
	 */
	public JavaSpace05 getSpace() {
		return space;
	}

	/**
	 * <p>
	 * Assigns the JavaSpace used by this distributed array.
	 * </p>
	 * 
	 * @param space
	 *            the JavaSpace to set
	 * @return nothing
	 */
	public void setSpace(JavaSpace05 space) {
		this.space = space;
	}
}
