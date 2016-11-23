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
package sorcer.util;

import net.jini.id.Uuid;
import sorcer.core.context.model.ent.Entry;
import sorcer.service.Fidelity;
import sorcer.service.Mogram;
import sorcer.service.Signature;

/**
 * Created by Mike Sobolewski on 10/28/16.
 */
public class Pools {

	/** Configuration component name for service mogram. */
	public static final String COMPONENT = Pools.class.getName();

	public static final String FI_POOL = "fiPool";

	public static final String FI_PROJECTIONS = "projections";

	// a map of fidelities to configure mograms of this environment
	final public static Pool<Uuid, Pool<Fidelity, Fidelity>> fiPool = new Pool<>();

	// a map of signatures to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Signature>> sigPool = new Pool<>();

	// a map of entries to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Entry<Object>>> entPool = new Pool<>();

	// a map of entries to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Entry<Object>>> derivativePool = new Pool<>();

	public static Pool<Fidelity, Fidelity> getFiPool(Uuid mogramId) {
		return fiPool.get(mogramId);
	}

	public static Pool<Fidelity, Fidelity> getFiPool(Mogram mogram) {
		return fiPool.get(mogram.getId());
	}

	public static void putFiPool(Mogram mogram, Pool<Fidelity, Fidelity> pool) {
		fiPool.put(mogram.getId(), pool);
	}

	public static Pool<String, Signature> getSigPool(Mogram mogram) {
		return sigPool.get(mogram.getId());
	}

	public static void putSigPool(Mogram mogram, Pool<String, Signature>  pool) {
		sigPool.put(mogram.getId(), pool);
	}

	public static Pool<String, Entry<Object>> getEntPool(Mogram mogram) {
		return entPool.get(mogram.getId());
	}

	public static void putEntPool(Mogram mogram, Pool<String, Entry<Object>>  pool) {
		entPool.put(mogram.getId(), pool);
	}

	public static Pool<String, Entry<Object>> getDerivativePool(Mogram mogram) {
		return derivativePool.get(mogram.getId());
	}

	public static void putDerivativePool(Mogram mogram, Pool<String, Entry<Object>> pool) {
		derivativePool.put(mogram.getId(), pool);
	}
}
