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
import sorcer.core.context.model.ent.Config;
import sorcer.core.context.model.ent.Function;
import sorcer.service.*;

import java.util.*;

/**
 * Created by Mike Sobolewski on 10/28/16.
 */
public class Pools {

	/** Configuration component key for service mogram. */
	public static final String COMPONENT = Pools.class.getName();

	public static final String FI_POOL = "fiPool";

	public static final String FI_PROJECTIONS = "projections";

	// a pool of fidelities to configure mograms of this environment
	final public static Pool<Uuid, Pool<Fidelity, Fidelity>> fiPool = new Pool<>();

	// a pool of signatures to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Signature>> sigPool = new Pool<>();

	// a pool of entries to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Function<Object>>> entPool = new Pool<>();

	// a pool of mograms to configure top-level mograms of this environment
	final public static Pool<Uuid, Pool<String, Mogram>> mogPool = new Pool<>();

	// a pool of entries to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Function<Object>>> derivativePool = new Pool<>();

	// a pool of setup configurations  for mograms of this environment
	final public static Pool<Uuid, Pool<String, Config>> configPool = new Pool<>();

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

	public static Pool<String, Function<Object>> getEntPool(Mogram mogram) {
		return entPool.get(mogram.getId());
	}

	public static void putEntPool(Mogram mogram, Pool<String, Function<Object>>  pool) {
		entPool.put(mogram.getId(), pool);
	}

	public static Pool<String, Mogram> getMogPool(Mogram mogram) {
		return mogPool.get(mogram.getId());
	}

	public static void putMogPool(Mogram mogram, Pool<String, Mogram>  pool) {
		mogPool.put(mogram.getId(), pool);
	}

	public static Pool<String, Function<Object>> getDerivativePool(Mogram mogram) {
		return derivativePool.get(mogram.getId());
	}

	public static void putDerivativePool(Mogram mogram, Pool<String, Function<Object>> pool) {
		derivativePool.put(mogram.getId(), pool);
	}

	public static Pool<String, Config> getConfigPool(Mogram mogram) {
		return configPool.get(mogram.getId());
	}

	public static void putConfigPool(Mogram mogram, Pool<String, Config>  pool) throws ContextException {
		Pool<String, Config> extended = expendConfigPool(pool);
		configPool.put(mogram.getId(), extended);
	}

	private static Pool<String, Config> expendConfigPool(Pool<String, Config>  pool) throws ContextException {
		Config config = null;
		Iterator<Map.Entry<String, Config>> i = pool.entrySet().iterator();
		Map.Entry<String, Config> entry;
		List<String> removedKeys = new ArrayList<>();
		List<String> expandedKeys = new ArrayList<>();
		while (i.hasNext()) {
			entry = i.next();
			config = entry.getValue();
			for (Setup s : config.get()) {
				if (s.getContext() == null) {
					removedKeys.add(s.getName());
					expandedKeys.add(entry.getKey());
				}
			}
		}
		for (int j = 0; j < removedKeys.size(); j++) {
			pool.get(expandedKeys.get(j)).addAll(pool.get(removedKeys.get(j)).get());
			pool.get(expandedKeys.get(j)).remove(removedKeys.get(j));
		}
		return pool;
	}
}
