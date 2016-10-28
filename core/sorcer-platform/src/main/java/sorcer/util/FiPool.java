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
import sorcer.service.Fidelity;
import sorcer.service.ServiceFidelity;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Mike Sobolewski on 10/28/16.
 */
public class FiPool {

	/** Configuration component name for service mogram. */
	public static final String COMPONENT = FiPool.class.getName();

	public static final String FI_POOL = "fiPool";

	// a map of fidelities to configure mograms of this environment
	final public static Map<Uuid, Map<Fidelity, ServiceFidelity>> fiPool = new Hashtable<>();


	public static Map<Fidelity, ServiceFidelity> get(Uuid mogramId) {
		return fiPool.get(mogramId);
	}

	public static void put(Uuid mogramId, Map<Fidelity, ServiceFidelity> mogramFis) {
		fiPool.put(mogramId,mogramFis);
	}

}
