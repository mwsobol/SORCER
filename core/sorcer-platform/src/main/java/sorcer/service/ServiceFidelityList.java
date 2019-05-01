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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Mike Sobolewski on 5/19/16.
 */
public class ServiceFidelityList extends ArrayList<ServiceFidelity> implements Arg {

	static final long serialVersionUID = 1L;

	private static int count = 0;

	private String name;

	public ServiceFidelityList() {
		super();
		count++;
	}

	public ServiceFidelityList(int size) {
		super(size);
	}

	public ServiceFidelityList(Set<ServiceFidelity> fiSet) {
		addAll(fiSet);
	}

	public ServiceFidelityList(ServiceFidelity... array) {
		super();
		for (ServiceFidelity mf : array) {
			add(mf);
		}
	}

	public ServiceFidelityList(ServiceFidelityList... fiLists) {
		super();
		for (ServiceFidelityList fl : fiLists) {
			addAll(fl);
		}
	}

	public ServiceFidelityList(Fidelity... fiArray) {
		super();
		for (Fidelity fi : fiArray) {
			add(new ServiceFidelity(fi));
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

	public static ServiceFidelityList selectFidelities(Arg[] entries) {
		ServiceFidelityList out = new ServiceFidelityList();
		for (Arg a : entries) {
			if (a instanceof ServiceFidelity && ((ServiceFidelity)a).fiType == ServiceFidelity.Type.SELECT) {
				out.add((ServiceFidelity)a);
			} else if (a instanceof ServiceFidelityList) {
				out.addAll((ServiceFidelityList)a);
			}
		}
		return out;
	}

	public String toString() {
		int tally = size();
		StringBuilder sb = new StringBuilder("fis(");
		if (tally > 0) {
			sb.append("metaFi(\"").append(get(0).getPath()).append("\", \"");
			if (tally == 1)
				sb.append(get(0).getName()).append("\")");
			else
				sb.append(get(0).getName()).append("\"), ");

			for (int i = 1; i < tally - 1; i++) {
				sb.append("metaFi(\"").append(get(i).getPath()).append("\", \"");
				if (tally == 1)
					sb.append(get(i).getName()).append("\")");
				else
					sb.append(get(i).getName()).append("\"), ");
			}

			if (tally > 1) {
				sb.append("metaFi(\"").append(get(tally - 1).getPath()).append("\", \"");
				sb.append(get(tally - 1).getName()).append("\")");
			}

		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Object execute(Arg... args) throws ServiceException, RemoteException {
		return this;
	}
}
