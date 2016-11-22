/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

package sorcer.util;

import sorcer.service.Fi;

import java.rmi.RemoteException;
import java.util.Hashtable;

/**
 * This class defines pools of multifidelities to be injected in deployment
 * of mograms based on a pool types.
 */
public class Pool<K,V> extends Hashtable<K,V> {

	private Fi.Type fiType = Fi.Type.SELECT;
	/**
	 * Constructs a new, empty hashtable with the specified initial capacity
	 * and default load factor (0.75).
	 *
	 * @param     initialCapacity   the initial capacity of the hashtable.
	 * @exception IllegalArgumentException if the initial capacity is less
	 *              than zero.
	 */
	public Pool(int initialCapacity) {
		super(initialCapacity, 0.75f);
	}

	/**
	 * Constructs a new, empty hashtable with a default initial capacity (11)
	 * and load factor (0.75).
	 */
	public Pool() {
		super(11, 0.75f);
	}


	public Fi.Type getFiType() {
		return fiType;
	}

	public void setFiType(Fi.Type fiType) {
		this.fiType = fiType;
	}

}
