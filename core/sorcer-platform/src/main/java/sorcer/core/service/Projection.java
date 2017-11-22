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

import sorcer.service.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mike Sobolewski on 6/26/16.
 */
public class Projection extends ServiceFidelity {

	public Projection(Fidelity fidelity) {
		this.fiName = fidelity.getName();
		this.path = fidelity.getPath();
	}

	public Projection(Fidelity[] fidelities) {
		super();
		this.selects = Arrays.asList(fidelities);
	}

    public Projection(List<Service> fidelities) {
        super();
        this.selects = fidelities;
    }

	public Projection(ServiceFidelity fidelity) {
		this.fiName = fidelity.getName();
		this.path = fidelity.getPath();
		for (Object fi : fidelity.getSelects()) {
			if (fi.getClass() == Fidelity.class) {
				this.selects.add((Fidelity) fi);
			} else if (fi instanceof ServiceFidelity
					&& ((ServiceFidelity)fi).getFiType().equals(Type.META)) {
				this.selects.add(new Projection((ServiceFidelity)fi));
			}
		}
	}

	public Fidelity getSelect() {
		return (Fidelity) select;
	}

	public List<Service> getFidelities() {
		return selects;
	}

	public List<Fidelity> getFidelities(String nodeName) {
		if (fiName.equals(nodeName)) {
			return selectFidelities();
		} else {
			for (Object fi : selects) {
				if (fi instanceof Projection) {
					if (((Projection) fi).getName().equals(nodeName)) {
						return ((Projection) fi).selectFidelities();
					}
				}
			}
		}
		return new ArrayList();
	}

	public void setFidelities(FidelityList fidelities) {
		List<Service> sl = new ArrayList<>();
		this.selects = fidelities.toServiceList();
	}

	@Override
	public String getName() {
		return fiName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Projection) {
			return selects.equals(((Projection)obj).getFidelities());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (selectsFidelityTypeOnly()) {
			List<Fidelity> fis = selectFidelities();
			int tally = fis.size();
			sb.append("fis(");
			if (tally > 0) {
				sb.append("fi(\"").append(fis.get(0).getName()).append("\", \"");
				if (tally == 1)
					sb.append(fis.get(0).getPath()).append("\")");
				else
					sb.append(fis.get(0).getPath()).append("\"), ");

				for (int i = 1; i < tally - 1; i++) {
					sb.append("fi(\"").append(fis.get(i).getName()).append("\", \"");
					if (tally == 1)
						sb.append(fis.get(i).getPath()).append("\")");
					else
						sb.append(fis.get(i).getPath()).append("\"), ");
				}

				if (tally > 1) {
					sb.append("fi(\"").append(fis.get(tally - 1).getName()).append("\", \"");
					sb.append(fis.get(tally - 1).getPath()).append("\")");
				}

			}
			sb.append(")");
		} else {
			sb.append("po(");
			sb.append(super.toString());
			sb.append(")");
		}
		return sb.toString();
	}

	public List<Fidelity> getAllFidelities() {
		List<Fidelity> out = new ArrayList();
		for (Service fi : selects) {
			if (fi instanceof Projection) {
				out.addAll(selectFidelities(((Projection)fi).getAllFidelities()));
			} else if (fi.getClass() == Fidelity.class) {
				out.add((Fidelity) fi);
			}
		}
		return out;
	}

	public static List<Fidelity> selectFidelities(Arg[] entries) {
		FidelityList out = new FidelityList();
		for (Arg s : entries) {
			if (s instanceof Projection) {
				out.addAll(((Projection) s).getAllFidelities());
			} else if (s instanceof FidelityList) {
				out.addAll((FidelityList) s);
			} else if (s instanceof Fidelity && ((Fidelity)s).fiType == Fidelity.Type.SELECT) {
				out.add((Fidelity)s);
			}
		}
		return out;
	}

//	public static List<Fidelity> selectFidelities(Service[] entries) {
//		FidelityList out = new FidelityList();
//		for (Service s : entries) {
//			if (s instanceof Projection) {
//				out.addAll(((Projection) s).getAllFidelities());
//			} else if (s instanceof FidelityList) {
//				out.addAll((FidelityList) s);
//			} else if (s instanceof Fidelity && ((Fidelity)s).fiType == Fidelity.Type.SELECT) {
//				out.add((Fidelity)s);
//			}
//		}
//		return out;
//	}

	public ServiceFidelity[] toFidelityArray() {
		List<Fidelity> allFi = getAllFidelities();
		ServiceFidelity[] sfis = new ServiceFidelity[allFi.size()];
		allFi.toArray(sfis);
		return sfis;
	}

}
