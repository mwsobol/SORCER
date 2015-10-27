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

import sorcer.core.invoker.Observable;
import sorcer.service.Arg;
import sorcer.service.Fidelity;
import sorcer.service.Identifiable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Mike Sobolewski on 10/26/15.
 */
public class MetaFidelity<T extends Arg> extends Observable implements Arg, Serializable {

    private Fidelity<T> mFi;

    public MetaFidelity(Fidelity  fi) {
        mFi = fi;
    }

    public MetaFidelity(FidelityManager manager) {
        addObserver(manager);
    }

    public Fidelity<T> getMultiFi() {
        return mFi;
    }

    public void setMultiFi(Fidelity<T> mFi) {
        this.mFi = mFi;
    }

    public T getSelection() {
        return mFi.getSelection();
    }

    public void setSelection(T selection) {
        mFi.setSelection(selection);
    }

    public T getSelect(String name) {
        return mFi.getSelect(name);
    }

    public List<T> getSelects() {
        return mFi.getSelects();
    }

    public void setSelects(List<T> selects) {
        mFi.setSelects(selects);;
    }

    public String getPath() {
        return mFi.getPath();
    }

    public void setPath(String fidelityPath) {
        mFi.setPath(fidelityPath);;
    }

    public String getName() {
        return mFi.getName();
    }

    public void setName(String name) {
        mFi.setName(name);
    }

    @Override
    public String toString() {
        return mFi.getName() + (mFi.getPath() != null ?
                "@" + mFi.getPath() + " " : " ") + mFi.getSelects();
    }

    public int size() {
        return mFi.getSelects().size();
    }
}
