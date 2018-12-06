/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.service;

import net.jini.id.Uuid;
import sorcer.core.context.ControlContext;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Mike Sobolewski
 */
abstract public class CompoundExertion extends ServiceExertion implements Mogram {

	/**
	 * Component mograms of this job (the Composite Design pattern)
	 */
	protected List<Mogram> mograms = new ArrayList<Mogram>();

	public CompoundExertion() {
		this("compound xrt=" + count++);
	}

	public CompoundExertion(String name) {
		super(name);
		mograms = new ArrayList<Mogram>();
	}

	public boolean isCompound() {
		return true;
	};

	public int size() {
		return   mograms.size();
	};

	public void reset(int state) {
		for(Mogram e : mograms)
			((ServiceExertion)e).reset(state);

		this.setStatus(state);
	}

	/**
	 * Replaces the exertion at the specified position in this list with the
	 * specified element.
	 */
	public void setMogramAt(Mogram ex, int i) {
		mograms.set(i, ex);
	}

	public Exertion getMasterExertion() {
		Uuid uuid = null;
		try {
			uuid = (Uuid) controlContext.getValue(ControlContext.MASTER_EXERTION);
		} catch (ContextException ex) {
			ex.printStackTrace();
		}
		if (uuid == null
				&& controlContext.getFlowType().equals(ControlContext.SEQUENTIAL)) {
			return (size() > 0) ? get(size() - 1) : null;
		} else {
			Exertion master = null;
			for (int i = 0; i < size(); i++) {
				if (((ServiceExertion) get(i)).getId().equals(
						uuid)) {
					master = get(i);
					break;
				}
			}
			return master;
		}
	}

	public Mogram removeExertion(Mogram mogram) throws ContextException {
		// int index = ((ExertionImpl)exertion).getIndex();
		mograms.remove(mogram);
		controlContext.deregisterExertion(this, mogram);
		return mogram;
	}

	public void remove(int index) throws ContextException {
		removeExertion(get(index));
	}

	/**
	 * Returns the exertion at the specified index.
	 */
	public Exertion get(int index) {
		return (Exertion) mograms.get(index);
	}

	public void setMograms(List<Mogram> mograms) {
		this.mograms = mograms;

	}

	public Mogram addExertion(Exertion exertion, int priority) throws ExertionException {
		addMogram(exertion);
		controlContext.setPriority(exertion, priority);
		return this;
	}

	/**
	 * Returns all component <code>Mograms</code>s of this composite exertion.
	 *
	 * @return all component mograms
	 */
	public List<Mogram> getMograms() {
		return mograms;
	}

	public boolean hasChild(String childName) {
		for (Mogram ext : mograms) {
			if (ext.getName().equals(childName))
				return true;
		}
		return false;
	}

	public Mogram getChild(String childName) {
		for (Mogram ext : mograms) {
			if (ext.getName().equals(childName))
				return ext;
		}
		return null;
	}

	public int compareByIndex(Exertion e) {
		if (this.getIndex() > ((CompoundExertion) e).getIndex())
			return 1;
		else if (this.getIndex() < ((CompoundExertion) e).getIndex())
			return -1;
		else
			return 0;
	}

	@Override
	public Object get(String component) {
		for (Mogram mog : mograms) {
			if (mog.getName().equals(component)) {
				return mog;
			}
		}
		return null;
	}

	abstract public Mogram getComponentMogram(String path);
	
	abstract public Context getComponentContext(String path) throws ContextException;

}
