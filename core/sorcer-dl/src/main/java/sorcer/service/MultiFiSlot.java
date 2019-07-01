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

package sorcer.service;

import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;

/**
 * An pair of objects as an identifiable multifidelity containers.
 *
 * @author Mike Sobolewski
 */
public class MultiFiSlot<K, O> extends Slot<K, O> implements Getter<O> {
	private  static final long serialVersionUID =  1L;

    // selectable carrier (fidelity) of out
	protected Object impl;

    protected Fi multiFi;

    protected Morpher morpher;

    protected Object annotation;

    // used for returning the requested value of this type
    protected Class valClass;

    protected Context scope;

    // when out is still valid
    protected boolean isValid = false;

    // when this slot has changed
    protected boolean isChanged = false;

    // if a value is computed then isCached is true - computed only one for all
    protected boolean isCached = false;

    public Integer index;

    protected Functionality.Type type = Functionality.Type.VAL;

    // default instance new Return(Context.RETURN);
    protected Context.Return contextReturn;

    public MultiFiSlot() {
	}

    public MultiFiSlot(K key) {
        this.key = key;
    }

    public MultiFiSlot(K key, Object item) {
        if(key==null)
            throw new IllegalArgumentException("key must not be null");
        this.key = key;
        if (item instanceof Fi) {
            multiFi = (Fi) item;
            this.impl = multiFi.get(0);
        } else {
            this.out = (O) item;
            this.impl = item;
        }
        isValid = true;
	}

	public Object getImpl() {
        return impl;
	}

	@Override
    public O getData(Arg... args) throws ContextException {
        if (out != null) {
            return out;
        } else {
            return (O) impl;
        }
    }

    public void setImpl(Object impl) {
		this.impl = impl;
	}

	public Object execute(Arg... entries) throws ServiceException, RemoteException {
		return impl;
	}

    /**
     * Returns the index assigned by the container.
     */
    public int getIndex() {
        return (index == null) ? -1 : index;
    }

    public void setIndex(int i) {
        index = i;
    }

    public Functionality.Type getType() {
        return type;
    }

    public Object annotation() {
        return annotation;
    }

    public void annotation(Object annotation) {
        this.annotation = annotation;
    }

    public void setType(Functionality.Type type) {
        this.type = type;
    }

    public Object getAnnotation() {
        return annotation;
    }

    public Context getScope() {
        return scope;
    }

    public void setScope(Context scope) {
        this.scope = scope;
    }

    public void initScope(Context scope) {
        this.scope = scope;
        if (impl instanceof Routine) {
            ((Routine) impl).setContext(scope);
        }
    }
    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean state) {
        isValid = state;
        if (impl instanceof MultiFiSlot) {
            ((MultiFiSlot) impl).isValid = state;
        }
    }

    public Morpher getMorpher() {
        return morpher;
    }

    public void setMorpher(Morpher morpher) {
        this.morpher = morpher;
    }

    public boolean isCached() {
        return isCached;
    }

    public void setCached(boolean cached) {
        isCached = cached;
    }

    public void setChanged(boolean state) {
        isChanged = state;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public Fi getMultiFi() {
        return multiFi;
    }

    public void setMultiFi(ServiceFidelity multiFi) {
        this.multiFi = multiFi;
    }

    public Class getValClass() {
        return valClass;
    }

    public void setValClass(Class valClass) {
        this.valClass = valClass;
    }

    public String fiName() {
        return ((Identifiable)multiFi.getSelect()).getName();
    }

    public Context.Return getContextReturn() {
        return contextReturn;
    }

    public void setContextReturn() {
        this.contextReturn = new Context.Return();
    }

    public void setContextReturn(String returnPath) {
        this.contextReturn = new Context.Return(returnPath);
    }

    public void setContextReturn(Context.Return contextReturn) {
        this.contextReturn = contextReturn;
    }

    public void setContextReturn(String path, Signature.Direction direction) {
        contextReturn = new Context.Return(path, direction);
    }

    public boolean isAnnotated() {
        return annotation != null;
    }

    public boolean equals(Object object) {
        if (object instanceof MultiFiSlot) {
            if (impl != null && ((MultiFiSlot) object).impl == null) {
                return false;
            } else if (impl == null && ((MultiFiSlot) object).impl != null) {
                return false;
            } else if (((MultiFiSlot) object).key.equals(key)
                    && ((MultiFiSlot) object).impl == impl) {
                return true;
            }  else if (((MultiFiSlot) object).key.equals(key)
                    && ((MultiFiSlot) object).impl.equals(impl)) {
                return true;
            }
        }
        return false;
    }

    protected Object realizeFidelity(Fi fidelity) {
        // reimplement in suclasses
        // define how the select of a fidelity is implemented by this association
        impl = fidelity.getSelect();
        return impl;
    }
}