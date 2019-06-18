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

package sorcer.core.context;

import java.rmi.RemoteException;

import net.jini.space.JavaSpace05;
import sorcer.service.ContextException;
import sorcer.service.SpaceContext;
import sorcer.service.space.SpaceAccessor;
import sorcer.space.array.DistribArray05;

/**
 * SpaceContext persists its context nodes in a distributed array in JavaSpace with 
 * the key provided in the constructor. In the data context requestReturn values correspond
 * to indices of elements in the distributed array. 
 */

public class SharedAssociativeContext extends ServiceContext implements SpaceContext {
	private DistribArray05 spaceElements;
	private String spaceName;
	
	public SharedAssociativeContext(String spaceName) {
		super();
		this.spaceName = spaceName;
		JavaSpace05 space = SpaceAccessor.getSpace(spaceName);
		spaceElements = new DistribArray05(space, "" + mogramId);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#writeValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object writeValue(String path, Object value) throws ContextException, RemoteException {
		try {
			int index = spaceElements.append(value);
			putValue(path, spacePrefix + index);
		} catch (Exception e) {
			throw new ContextException(e);
		} 
		return value;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#readValue(java.lang.String)
	 */
	@Override
	public Object readValue(String path) throws ContextException, RemoteException {
		Object value = super.getValue(path);
		int index = -1;
		if (value instanceof String && ((String) value).startsWith(spacePrefix)) {
			index = new Integer(((String) value).substring(spacePrefix.length()));

		}
		if (index >= 0) {
			try {
				value = spaceElements.readElement(index);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#takeValue(java.lang.String)
	 */
	@Override
	public Object takeValue(String path) throws ContextException, RemoteException {
		Object value = super.getValue(path);
		int index = -1;
		if (value instanceof String && ((String) value).startsWith(spacePrefix)) {
			index = new Integer(((String) value).substring(spacePrefix.length()));

		}
		if (index >= 0) {
			try {
				value = spaceElements.takeElement(index);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
		return value;
	}
	
	private void setSpace() {
		JavaSpace05 space = SpaceAccessor.getSpace(spaceName);
		spaceElements.setSpace(space);
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#aliasValue(java.lang.String)
	 */
	@Override
	public Object aliasValue(String path, String alias) throws ContextException,
			RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#share()
	 */
	@Override
	public void share() throws ContextException, RemoteException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.SpaceContext#unshare()
	 */
	@Override
	public void unshare() throws ContextException, RemoteException {
		// TODO Auto-generated method stub
		
	}
}