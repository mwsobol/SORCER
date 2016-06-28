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

package sorcer.core.service;

import sorcer.service.Arg;
import sorcer.service.Fidelity;
import sorcer.service.FidelityList;
import sorcer.service.ServiceFidelity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike Sobolewski on 6/26/16.
 */
public class Projection<T extends Fidelity> extends Fidelity<T> {

    private Fidelity fidelity;

	private FidelityList fidelities;


	public Projection(Fidelity fidelity) {
		super();
		this.fidelity = fidelity;
	}

	public Projection(FidelityList fiList) {
		super();
		this.fidelity = fidelity;
		this.fidelities = fiList;
	}

	public Projection(Fidelity... fidelities) {
		super();
		this.fidelities = new FidelityList(fidelities);
	}

	public Fidelity getfidelity() {
		return fidelity;
	}

    public void setFidelity(Fidelity fidelity) {
		this.fidelity = fidelity;
	}

	public List<Fidelity> getFidelities() {
		return fidelities;
	}

	public void setFidelities(FidelityList fidelities) {
		this.fidelities = fidelities;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Projection) {
			return fidelities.equals(((Projection)obj).getFidelities());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("po(");
		if (fidelity == null) {
			int tally = fidelities.size();
			if (tally > 0) {
				sb.append("fi(\"").append(fidelities.get(0).getPath()).append("\", \"");
				if (tally == 1)
					sb.append(fidelities.get(0).getName()).append("\")");
				else
					sb.append(fidelities.get(0).getName()).append("\"), ");

				for (int i = 1; i < tally - 1; i++) {
					sb.append("fi(\"").append(fidelities.get(i).getPath()).append("\", \"");
					if (tally == 1)
						sb.append(fidelities.get(i).getName()).append("\")");
					else
						sb.append(fidelities.get(i).getName()).append("\"), ");
				}

				if (tally > 1) {
					sb.append("fi(\"").append(fidelities.get(tally - 1).getPath()).append("\", \"");
					sb.append(fidelities.get(tally - 1).getName()).append("\")");
				}

			}
			sb.append(")");
		}
		return sb.toString();
	}

	public List<Fidelity> getAllFidelities() {
		List<Fidelity> out = new ArrayList();
		for (Fidelity fi : fidelities) {
			if (fi instanceof Projection) {
				out.addAll(((Projection)fi).getAllFidelities());
			} else if (fi.getClass() == Fidelity.class) {
				out.add(fi);
			}
		}
		return out;
	}

	public static List<Fidelity> selectFidelities(Arg[] entries) {
		FidelityList out = new FidelityList();
		for (Arg a : entries) {
			if (a instanceof Projection) {
				out.addAll(((Projection) a).getAllFidelities());
			} else if (a instanceof FidelityList) {
				out.addAll((FidelityList) a);
			} else if (a instanceof Fidelity && ((Fidelity)a).type == Fidelity.Type.SELECT) {
				out.add((Fidelity)a);
			}
		}
		return out;
	}

	public ServiceFidelity[] toFidelityArray() {
		List<Fidelity> allFi = getAllFidelities();
		ServiceFidelity[] sfis = new ServiceFidelity[allFi.size()];
		allFi.toArray(sfis);
		return sfis;
	}

}
