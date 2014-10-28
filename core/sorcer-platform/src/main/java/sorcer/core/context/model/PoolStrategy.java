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

package sorcer.core.context.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import sorcer.service.Signature;
import sorcer.service.Strategy.Flow;

/**
 * @author Mike Sobolewski
 */
public class PoolStrategy implements Serializable {
	static final long serialVersionUID = -2199530268313502745L;
	sorcer.service.Signature builder;
	List<Pool> pools = new ArrayList<Pool>(2);
	Flow flow;
	int capacity = 10;
	int load;

	public Signature getBuilder() {
		return builder;
	}

	public void setBuilder(sorcer.service.Signature signature) {
		builder = signature;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getLoad() {
		return load;
	}

	public void setLoad(int load) {
		this.load = load;
	}

	public Flow getFlow() {
		return flow;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	public List<Pool> getPools() {
		return pools;
	}

	public void setPools(List<Pool> pools) {
		this.pools = pools;
	}

	public String toString() {
		return "Strategy: [flow=" + flow + ", capacity=" + capacity + ", load="
				+ load + ", builder=" + builder + "]";
	}
}
