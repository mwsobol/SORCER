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

import java.util.Hashtable;
import java.util.Map;

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

	// a map of entries to configure mograms of this environment
	final public static Pool<Uuid, Pool<String, Entry<Object>>> entPool = new Pool<>();

	public static Pool<Fidelity, Fidelity> getFiPool(Uuid mogramId) {
		return fiPool.get(mogramId);
	}

	public static void putFiPool(Uuid mogramId, Pool<Fidelity, Fidelity> mogramFiPool) {
		fiPool.put(mogramId, mogramFiPool);
	}

	public static Pool<String, Entry<Object>> getEntPool(Uuid mogramId) {
		return entPool.get(mogramId);
	}

	public static void putEntPool(Uuid mogramId, Pool<String, Entry<Object>> mogramEntPool) {
		entPool.put(mogramId, mogramEntPool);
	}

}
