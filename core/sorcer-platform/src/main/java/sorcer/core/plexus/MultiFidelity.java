/*
* Copyright 2013 the original author or authors.
* Copyright 2013, 2014 Sorcersoft.com S.A.
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

package sorcer.core.plexus;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.invoker.Observable;
import sorcer.service.Arg;
import sorcer.service.Fidelity;
import sorcer.service.Identifiable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Mike Sobolewski on 10/26/15.
 */
public class MultiFidelity<T extends Arg> extends Observable implements Identifiable, Arg, Serializable {

    // fidelity of fidelities T  taht is observable
    private Fidelity<T> fidelity;

    private Uuid id = UuidFactory.generate();

    public MultiFidelity(Fidelity fi) {
        fidelity = fi;
    }

    public MultiFidelity(FidelityManager manager) {
        addObserver(manager);
    }

    public Fidelity<T> getFidelity() {
        return fidelity;
    }

    public void setMultiFidelity(Fidelity<T> fi) {
        this.fidelity = fi;
    }

    public T getSelection() {
        return fidelity.getSelection();
    }

    public void setSelection(T selection) {
        fidelity.setSelection(selection);
    }

    public T getSelect(String name) {
        return fidelity.getSelect(name);
    }

    public List<T> getSelects() {
        return fidelity.getSelects();
    }

    public void setSelects(List<T> selects) {
        fidelity.setSelects(selects);;
    }

    public String getPath() {
        return fidelity.getPath();
    }

    public void setPath(String fidelityPath) {
        fidelity.setPath(fidelityPath);;
    }

    @Override
    public Object getId() {
        return id;
    }

    public String getName() {
        return fidelity.getName();
    }

    public void setName(String name) {
        fidelity.setName(name);
    }

    @Override
    public String toString() {
        return fidelity.getName() + (fidelity.getPath() != null ?
                "@" + fidelity.getPath() + " " : " ") + fidelity.getSelects();
    }

    public int size() {
        return fidelity.getSelects().size();
    }
}
