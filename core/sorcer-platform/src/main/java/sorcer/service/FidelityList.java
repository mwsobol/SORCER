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
import java.util.List;
import java.util.Set;

/**
 * Created by Mike Sobolewski on 5/19/16.
 */
public class FidelityList extends ArrayList<Fidelity> implements Arg {

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

    public FidelityList(Set<Fidelity> fiSet) {
        addAll(fiSet);
    }

    public FidelityList(Fidelity... array) {
        super();
        for (Fidelity mf : array) {
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

    public Fidelity[] toFidelityArray() {
        Fidelity[] sfis = new Fidelity[this.size()];
        this.toArray(sfis);
        return sfis;
    }

    public static FidelityList selectFidelities(Arg[] entries) {
        FidelityList out = new FidelityList();
        for (Arg a : entries) {
            if (a instanceof Fidelity && ((Fidelity)a).fiType == Fidelity.Type.SELECT) {
                out.add((Fidelity)a);
            } else if (a instanceof FidelityList) {
                out.addAll((FidelityList)a);
            }
        }
        return out;
    }

    public List<Service> toServiceList() {
        List<Service> sl = new ArrayList<>();
        sl.addAll(this);
        return sl;
    }

    public String toString() {
        int tally = size();
        StringBuilder sb = new StringBuilder("fis(");
        if (tally > 0) {
            sb.append("fi(\"").append(get(0).getName()).append("\", \"");
            if (tally == 1)
                sb.append(get(0).getPath()).append("\")");
            else
                sb.append(get(0).getPath()).append("\"), ");

            for (int i = 1; i < tally - 1; i++) {
                sb.append("fi(\"").append(get(i).getName()).append("\", \"");
                if (tally == 1)
                    sb.append(get(i).getPath()).append("\")");
                else
                    sb.append(get(i).getPath()).append("\"), ");
            }

            if (tally > 1) {
                sb.append("fi(\"").append(get(tally - 1).getName()).append("\", \"");
                sb.append(get(tally - 1).getPath()).append("\")");
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
