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
package sorcer.service;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Mike Sobolewski on 5/19/16.
 */
public class FidelityList extends ArrayList<ServiceFidelity> implements Arg {

	static final long serialVersionUID = 1L;

	private static int count = 0;

	private String name;

	public FidelityList() {
		super();
		count++;
	}

	public FidelityList(int size) {
		super(size);
	}

	public FidelityList(Set<ServiceFidelity> fiSet) {
		addAll(fiSet);
	}

	public FidelityList(ServiceFidelity... array) {
		super();
		for (ServiceFidelity mf : array) {
			add(mf);
		}
	}

	public FidelityList(FidelityList... fiLists) {
		super();
		for (FidelityList fl : fiLists) {
			addAll(fl);
		}
	}

	@Override
	public String getName() {
		if (name == null)
			return getClass().getName()+ "-" + count;
		else
		 return name;
	}

	public ServiceFidelity[] toFidelityArray() {
		ServiceFidelity[] sfis = new ServiceFidelity[this.size()];
		this.toArray(sfis);
		return sfis;
	}

	public static FidelityList selectFidelities(Arg[] entries) {
		FidelityList out = new FidelityList();
		for (Arg a : entries) {
			if (a instanceof ServiceFidelity && ((ServiceFidelity)a).type == ServiceFidelity.Type.EMPTY) {
				out.add((ServiceFidelity)a);
			} else if (a instanceof FidelityList) {
				out.addAll((FidelityList)a);
			}
		}
		return out;
	}
}
