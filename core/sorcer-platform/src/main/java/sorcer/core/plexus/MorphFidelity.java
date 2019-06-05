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
import sorcer.service.modeling.Functionality;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Mike Sobolewski on 04/26/16.
 */
public class MorphFidelity<T> extends Observable implements Identifiable, MorphFi<T>, Arg {

    // fidelity of fidelities that are observable
    private Fidelity<T> fidelity;

    private Morpher morpher;

    private String path;

    private Fidelity morpherFidelity;

    private Uuid id = UuidFactory.generate();

    public MorphFidelity(Fidelity fi) {
        fidelity = fi;
        path = fi.getPath();
    }

    public MorphFidelity(Service... services) {
        this(new ServiceFidelity(services));
        boolean morpherFisSet = false;
        for (Object srv : fidelity.getSelects()) {
            if (srv instanceof Fidelity) {
                morpherFidelity = (Fidelity) srv;
                morpher = (Morpher) ((Entry)((Fidelity) srv).getSelects().get(0)).getImpl();
                morpherFisSet = true;
            }
        }
        if (morpherFisSet) {
            fidelity.getSelects().remove(morpherFidelity);
            fidelity.setSelect(fidelity.getSelects().get(0));
        }
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
        T selection = null;

        if (obj instanceof Ref) {
            selection = (T) ((Ref) obj).getValue();
        } else{
            selection = (T) obj;
        }
        return selection;
    }

    public T selectSelect(String name) throws ConfigurationException {
        return fidelity.selectSelect(name);
    }

    public void setMorpherSelect(String name) throws ConfigurationException {
        if (morpherFidelity != null) {
            morpherFidelity.selectSelect(name);
            morpher = (Morpher) ((Entry)morpherFidelity.getSelect()).getImpl();
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
    public void removeSelect(T select) {
        fidelity.removeSelect(select);
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Type getFiType() {
        return fidelity.getFiType();
    }

    @Override
    public void setChanged(boolean state) {
        fidelity.setChanged(state);
    }

    @Override
    public void clearFi() {
        fidelity.clearFi();
        fidelity.setSelect(null);
    }

    public T getSelect(String name) throws ConfigurationException {
        return fidelity.getSelect(name);
    }

    public List<T> getSelects() {
        return fidelity.getSelects();
    }

    public void setSelects(List<T> selects) {
        fidelity.setSelects(selects);
    }

    public Fidelity getMorpherFidelity() {
        return morpherFidelity;
    }

    public void setMorpherFidelity(Fidelity morpherFidelity) {
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
        if (morpher == null && fidelity.getSelect() instanceof Fidelity) {
            // the case of selectable morphers
            Object ent = ((Fidelity)fidelity.getSelect()).getSelect();
            if (ent instanceof Entry && ((Entry) ent).getType().equals(Functionality.Type.LAMBDA)) {
                morpher = (Morpher) ((Entry) ent).getImpl();
            }
        }
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
    
    public Object execute(Arg... args) throws ServiceException, RemoteException {
        if (fidelity.getSelect() instanceof Service) {
            return ((Service)fidelity.getSelect()).execute(args);
        } else return fidelity.getSelect();
    }

    public T get(int index) {
        return fidelity.getSelects().get(index);
    }

}

