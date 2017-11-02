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
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.core.exertion.AltMogram;
import sorcer.core.exertion.LoopMogram;
import sorcer.core.exertion.OptMogram;
import sorcer.core.signature.ObjectSignature;
import sorcer.util.SorcerUtil;
import sorcer.util.url.sos.SdbUtil;
import sorcer.service.Signature.ReturnPath;


import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;


/**
 * @author Mike Sobolewski
 */
public abstract class Block extends CompoundExertion {
	
	private URL contextURL;
	
	public Block(String name) {
		super(name);
	}

	public Block(String name, Signature signature) {
		super(name);
		try {
			ServiceFidelity sFi = new ServiceFidelity(signature);
			sFi.setSelect(signature);
			((ServiceFidelity)multiFi).getSelects().add(sFi);// Add the signature
			multiFi.setSelect(sFi);

			setContext(new ProcModel("block context: " + getName()));
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
	
	public abstract Block doBlock(Transaction txn, Arg... args) throws ExertionException,
		SignatureException, RemoteException, TransactionException, MogramException;
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#addMogram(sorcer.service.Exertion)
	 */
	@Override
	public Mogram addMogram(Mogram mogram) throws ExertionException {
		mograms.add(mogram);
		mogram.setIndex(mograms.indexOf(mogram));
		try {
			controlContext.registerExertion(mogram);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		mogram.setParentId(getId());
		return this;
	}

	public void setMograms(List<Mogram> mograms) {
		this.mograms = mograms;
	}

	public void setMograms(Mogram[] mograms) throws ExertionException {
		for (Mogram mo :mograms)
			addMogram(mo);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#execute(java.lang.String, sorcer.service.Arg[])
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
	 * @see sorcer.service.Exertion#getMograms()
	 */
	@Override
	public List<Mogram> getMograms() {
		return mograms;
	}

	public List<Mogram> getAllMograms() {
		return mograms;
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
		Iterator i = mograms.iterator();
		while (i.hasNext()) {
			ServiceExertion e = (ServiceExertion) i.next();
			if (visited.contains(e) || !e.isTree(visited)) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.ServiceExertion#getMograms(java.util.List)
	 */
	@Override
	public List<Mogram> getMograms(List<Mogram> exs) {
		for (Mogram e : mograms)
			((ServiceExertion) e).getMograms(exs);
		exs.add(this);
		return exs;
	}
	
	public URL persistContext() throws MogramException, SignatureException, ContextException {
		if (contextURL == null) {
			contextURL = SdbUtil.store(dataContext);
			dataContext = null;
		} else {
			SdbUtil.update(dataContext);
		}
		return contextURL;
	}

	/**
	 * Returns the number of mograms in this Block.
	 * 
	 * @return the number of mograms in this Block.
	 */
	public int size() {
		return mograms.size();
	}

	public void remove(int index) {
		new RuntimeException().printStackTrace();
		mograms.remove(index);
	}

	/**
	 * Replaces the exertion at the specified position in this list with the
     * specified element.
	 */
	public void setMogramAt(Mogram ex, int i) {
		mograms.set(i, ex);
	}
	
	/**
	 * Returns the exertion at the specified index.
	 */
	public Exertion get(int index) {
		return (Exertion) mograms.get(index);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.CompoundExertion#isCompound()
	 */
	@Override
	public boolean isCompound() {
		return true;
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

	public Mogram getComponentMogram(String path) {
		// TODO
		return getChild(path);
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
		Mogram exti = this;
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
		((Exertion)exti).getContext().putValue(contextPath, value);
		return value;
	}
	
	public void reset(int state) {
		for(Mogram e : mograms)
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
	public void substitute(Arg... entries)
			throws SetterException {
		try {
			for (Arg e : entries) {
				if (e instanceof Entry) {
					if (e.getName().indexOf(name) >= 0)
						putBlockValue(e.getName(), ((Entry) e).get());

					else
						super.putValue(e.getName(), ((Entry) e).get());
				}
			}
			updateConditions();
		} catch (ContextException ex) {
			ex.printStackTrace();
			throw new SetterException(ex);
		}
	}
	
	private void updateConditions() throws ContextException {
		for (Mogram e : mograms) {
			if (e instanceof Exertion && ((Exertion)e).isConditional()) {
				if (e instanceof OptMogram) {
					((OptMogram)e).getCondition().getConditionalContext().append(dataContext);
				} else if (e instanceof LoopMogram && ((LoopMogram) e).getCondition() != null) {
					((LoopMogram) e).getCondition().getConditionalContext().append(dataContext);
				} else if (e instanceof AltMogram) {
					for (OptMogram oe : ((AltMogram) e).getOptExertions()) {
						oe.getCondition().getConditionalContext().append(dataContext);
					}
				}
			}
		}
	}

	public Mogram clearScope() throws ContextException {
		Object[] paths = ((ServiceContext)getDataContext()).keySet().toArray();
		for (Object path : paths) {
			dataContext.removePath((String) path);
//			dataContext.getScope().removePath((String) path);
		}

		ReturnPath rp = dataContext.getReturnPath();
		if (rp != null && rp.path != null)
			dataContext.removePath(rp.path);

		List<Mogram> mograms = getAllMograms();
		Context cxt = null;
		for (Mogram mo : mograms) {
			if (mo instanceof Exertion )
				((ServiceContext)((Exertion)mo).getDataContext()).clearScope();

//			if (mo instanceof Exertion)
//				cxt = ((Exertion)mo).getContext();
//			else
//				cxt = (Context) mo;

//			if (!(mo instanceof Block)) {
//				try {
//					cxt.setScope(null);
//				} catch (RemoteException e) {
//					throw new ContextException(e);
//				}
//			}
//			try {
//				if (mo instanceof Exertion) {
//					((Exertion) mo).clearScope();
//					// set the initial scope from the block
//					mo.setScope((Context) dataContext.getScope());
//				}
//			} catch (RemoteException e) {
//				throw new ContextException(e);
//			}
		}

		// restore initial context
		if (dataContext.getInitContext() != null) {
			dataContext.append(dataContext.getInitContext());
		}

		return this;
	}

}
