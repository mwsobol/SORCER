/*
* Copyright 2016 SORCERsoft.org.
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
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Ref;
import sorcer.core.invoker.Observable;
import sorcer.service.*;
import sorcer.service.modeling.Duo;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Mike Sobolewski on 04/26/16.
 */
public class MorphFidelity<T> extends Observable implements Identifiable, Fi<T>, Arg {

    // fidelity of fidelities thatare observable
    private Fidelity<T> fidelity;

    private Morpher morpher;

    private String path;

    private ServiceFidelity morpherFidelity;

    private Uuid id = UuidFactory.generate();

    public MorphFidelity(Fidelity fi) {
        fidelity = fi;
        path = fi.getPath();
    }

    public MorphFidelity(FidelityManager manager) {
        addObserver(manager);
    }

    public Fidelity getFidelity() {
        return fidelity;
    }

    public void setFidelity(Fidelity fi) {
        this.fidelity = fi;
    }

    public T getSelect() {
        Object obj = fidelity.getSelect();;
        T select = null;

        if (obj instanceof Ref) {
            select = (T) ((Ref) obj).get();
        } else{
            select = (T) obj;
        }
        return select;
    }

    public void setSelect(String name) {
        fidelity.setSelect(name);
    }

    public void setMorpherSelect(String name) {
        if (morpherFidelity != null) {
            morpherFidelity.setSelect(name);
            morpher = (Morpher) ((Entry)morpherFidelity.getSelect()).getItem();
        }
    }

    @Override
    public void addSelect(T fidelity) {
        this.fidelity.addSelect(fidelity);
    }

    public void setSelect(T selection) {
        fidelity.setSelect(selection);
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Type getType() {
        return fidelity.getType();
    }

    @Override
    public void setChanged(boolean state) {
        fidelity.setChanged(state);
    }

    public T getSelect(String name) {
        return fidelity.getSelect(name);
    }

    public List<T> getSelects() {
        return fidelity.getSelects();
    }

    public void setSelects(List<T> selects) {
        fidelity.setSelects(selects);
    }

    public ServiceFidelity getMorpherFidelity() {
        return morpherFidelity;
    }

    public void setMorpherFidelity(ServiceFidelity morpherFidelity) {
        this.morpherFidelity = morpherFidelity;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String fidelityPath) {
        path = fidelityPath;
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

    public Morpher getMorpher() {
        return morpher;
    }

    public void setMorpher(Morpher morpher) {
        this.morpher = morpher;
    }

    @Override
    public String toString() {
        return fidelity.getName() + (fidelity.getPath() != null ?
                "@" + fidelity.getPath() + " " : " ") + fidelity.getSelects();
    }

    public int size() {
        return fidelity.getSelects().size();
    }

    @Override
    public Object execute(Arg... args) throws ServiceException, RemoteException {
        if (fidelity.getSelect() instanceof Service) {
            return ((Service)fidelity.getSelect()).execute(args);
        } else return fidelity.getSelect();
    }

    public T get(int index) {
        return fidelity.getSelects().get(index);
    }

    @Override
    public Duo act(Arg... args) throws ServiceException, RemoteException {
        return new Entry(path, fidelity);
    }

    @Override
    public Duo act(String entryName, Arg... args) throws ServiceException, RemoteException {
        return new Entry(entryName, fidelity);
    }
}

