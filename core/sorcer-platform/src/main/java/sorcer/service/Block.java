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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.co.tuple.Entry;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.exertion.AltExertion;
import sorcer.core.exertion.LoopExertion;
import sorcer.core.exertion.OptExertion;
import sorcer.util.SorcerUtil;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * @author Mike Sobolewski
 */
public abstract class Block extends ServiceExertion implements CompoundExertion {

	private List<Exertion> exertions = new ArrayList<Exertion>();
	
	private URL contextURL;
	
	public Block(String name) {
		super(name);
	}
	
	public Block(String name, Signature signature) {
		super(name);
		try {
			fidelity.add(signature);
			try {
				setContext(new ParModel("block context: " + getName()));
//				persistContext();
			} catch (Exception e) {
				throw new ExertionException(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Block(String name, Signature signature, Context context)
			throws SignatureException {
		this(name, signature);
		if (context != null)
			this.dataContext = (ServiceContext) context;
	}
	
	public abstract Block doBlock(Transaction txn) throws ExertionException,
		SignatureException, RemoteException, TransactionException;
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#addExertion(sorcer.service.Exertion)
	 */
	@Override
	public Exertion addExertion(Exertion ex) throws ExertionException {
		exertions.add(ex);
		((ServiceExertion) ex).setIndex(exertions.indexOf(ex));
		try {
			controlContext.registerExertion(ex);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		((ServiceExertion) ex).setParentId(getId());
		return this;
	}

	public void setExertions(List<Exertion> exertions) {
		this.exertions = exertions;
	}

	public void setExertions(Exertion[] exertions) throws ExertionException {
		for (Exertion e :exertions)
			addExertion(e);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#getValue(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public Object getValue(String path, Arg... args) throws ContextException {
		dataContext.getValue(path, args);
		return null;
	}

	@Override
	public Context getDataContext() throws ContextException {
		if (contextURL != null) {
			try {
				return (Context)contextURL.getContent();
			} catch (IOException e) {
				throw new ContextException(e);
			}
		} else {
			return dataContext;
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#getExertions()
	 */
	@Override
	public List<Exertion> getExertions() {
		return exertions;
	}

	public List<Exertion> getAllExertions() {
		return exertions;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.ServiceExertion#linkContext(sorcer.service.Context, java.lang.String)
	 */
	@Override
	public Context linkContext(Context context, String path)
			throws ContextException {
		dataContext.putLink(path + CPS + name, context);
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.ServiceExertion#linkControlContext(sorcer.service.Context, java.lang.String)
	 */
	@Override
	public Context linkControlContext(Context context, String path)
			throws ContextException {
		controlContext.putLink(path + CPS + name, context);
		return context;
	}

	public boolean isBlock() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.ServiceExertion#isTree(java.util.Set)
	 */
	@Override
	public boolean isTree(Set visited) {
		visited.add(this);
		Iterator i = exertions.iterator();
		while (i.hasNext()) {
			ServiceExertion e = (ServiceExertion) i.next();
			if (visited.contains(e) || !e.isTree(visited)) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.ServiceExertion#getExertions(java.util.List)
	 */
	@Override
	public List<Exertion> getExertions(List<Exertion> exs) {
		for (Exertion e : exertions)
			((ServiceExertion) e).getExertions(exs);
		exs.add(this);
		return exs;
	}
	
	public URL persistContext() throws ExertionException, SignatureException, ContextException {
		if (contextURL == null) {
			contextURL = SdbUtil.store(dataContext);
			dataContext = null;
		} else {
			SdbUtil.update(dataContext);
		}
		return contextURL;
	}

	/**
	 * Returns the number of exertions in this Block.
	 * 
	 * @return the number of exertions in this Block.
	 */
	public int size() {
		return exertions.size();
	}

	public void remove(int index) {
		new RuntimeException().printStackTrace();
		exertions.remove(index);
	}

	/**
	 * Replaces the exertion at the specified position in this list with the
     * specified element.
	 */
	public void setExertionAt(Exertion ex, int i) {
		exertions.set(i, ex);
	}
	
	/**
	 * Returns the exertion at the specified index.
	 */
	public Exertion get(int index) {
		return (Exertion) exertions.get(index);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.CompoundExertion#isCompound()
	 */
	@Override
	public boolean isCompound() {
		return true;
	}
	
	public boolean hasChild(String childName) {
		for (Exertion ext : exertions) {
			if (ext.getName().equals(childName))
				return true;
		}
		return false;
	}

	public Exertion getChild(String childName) {
		for (Exertion ext : exertions) {
			if (ext.getName().equals(childName))
				return ext;
		}
		return null;
	}

	public Object putBlockValue(String path, Object value) throws ContextException {
		String[] attributes = SorcerUtil.pathToArray(path);
		// remove the leading attribute of the current exertion
		if (attributes[0].equals(getName())) {
			// updated this context
			if ((attributes.length >= 2) && !hasChild(attributes[1])) {
				dataContext.putValue(path.substring(name.length() + 1), value);
				return value;
			}
			String[] attributes1 = new String[attributes.length - 1];
			System.arraycopy(attributes, 1, attributes1, 0,
					attributes.length - 1);
			attributes = attributes1;
		}
		String last = attributes[0];
		Exertion exti = this;
		for (String attribute : attributes) {
			if (((ServiceExertion) exti).hasChild(attribute)) {
				exti = ((CompoundExertion) exti).getChild(attribute);
				if (exti instanceof Task) {
					last = attribute;
					break;
				}
			} else {
				break;
			}
		}
		int index = path.indexOf(last);
		String contextPath = path.substring(index + last.length() + 1);
		exti.getContext().putValue(contextPath, value);
		return value;
	}
	
	public void reset(int state) {
		for(Exertion e : exertions)
			((ServiceExertion)e).reset(state);
		
		this.setStatus(state);
	}

	/**
	 *  TODO
	 * @param path
	 * @return
	 * @throws ContextException
	 */
	@Override
	public Context getComponentContext(String path) throws ContextException {
		return null;
	}

	@Override
	public ServiceExertion substitute(Arg... entries)
			throws SetterException {
		try {
			for (Arg e : entries) {
				if (e instanceof Entry) {
					if (((Entry) e).path().indexOf(name) >= 0)
						putBlockValue(((Entry) e).path(), ((Entry) e).value());

					else
						super.putValue(((Entry) e).path(), ((Entry) e).value());
				}
			}
			updateConditions();
		} catch (ContextException ex) {
			ex.printStackTrace();
			throw new SetterException(ex);
		}
		return this;
	}
	
	private void updateConditions() throws ContextException {
		for (Exertion e : exertions) {
			if (e.isConditional()) {
				if (e instanceof OptExertion) { 
					((OptExertion)e).getCondition().getConditionalContext().append(dataContext);
				} else if (e instanceof LoopExertion) {
					((LoopExertion) e).getCondition().getConditionalContext().append(dataContext);
				} else if (e instanceof AltExertion) {
					for (OptExertion oe : ((AltExertion) e).getOptExertions()) {
						oe.getCondition().getConditionalContext().append(dataContext);
					}
				}
			}
		}
	}
}
