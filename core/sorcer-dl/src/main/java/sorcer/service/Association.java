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

import sorcer.service.modeling.Data;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Getter;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * An pair of objects as an identifiable impl.
 *
 * @author Mike Sobolewski
 */
public class Association<K, I> implements Service, net.jini.core.entry.Entry, Data<I>, Getter, Serializable, Identifiable, Arg {
	private  static final long serialVersionUID =  1L;

	protected K key;

    protected I out;

    // carrier of out
	protected Object impl;

    protected Fi multiFi;

    protected Functionality.Type type = Functionality.Type.VAL;

    public Integer index;

    protected Object annotation;

    // used for returning the requested value of this type
    protected Class valClass;

    protected Context scope;

    // when scope of this entry is changed then is not valid
    protected boolean isValid = true;

    public Association() {
	}

    public Association(K key) {
        this.key = key;
    }

    public Association(K key, Object item) {
        if(key==null)
            throw new IllegalArgumentException("key must not be null");
        this.key = key;
        if (item instanceof Fi) {
            multiFi = (Fi) item;
            this.impl = multiFi.get(0);
        } else {
            this.out = (I) item;
            this.impl = item;
        }
	}

    public K getKey() {
		return key;
	}

    public K key() {
        return key;
    }

    public K path() {
        return key;
    }

	public void setKey(K key) {
		this.key = key;
	}

	public Object getImpl() {
		if (!isValid && multiFi != null) {
            impl = ((Association)multiFi.getSelect()).getImpl();
            isValid = true;
            return impl;
        } else {
            return impl;
        }
	}

    public I get(Arg... args) throws ContextException {
        return out;
    }

    public I getData(Arg... args) throws ContextException {
        if (out != null) {
            return out;
        } else {
            return (I) impl;
        }
    }

    public I asis() throws EvaluationException, RemoteException {
        return out;
    }

    public void setImpl(Object impl) {
		this.impl = impl;
	}

    public void set(I value) {
        this.out = value;
    }

	@Override
	public String toString() {
		return "[" + key + ":" + impl + "]";
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

    @Override
	public Object getId() {
		return key.toString();
	}

	@Override
	public String getName() {
		return key.toString();
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

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean state) {
        isValid = state;
        if (impl instanceof Association) {
            ((Association) impl).isValid = state;
        }
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

    @Override
    public int hashCode() {
        return 2 * 31 + key.hashCode();
    }

    public boolean equals(Object object) {
        if (object instanceof Association) {
            if (impl != null && ((Association) object).impl == null) {
                return false;
            } else if (impl == null && ((Association) object).impl != null) {
                return false;
            } else if (((Association) object).key.equals(key)
                    && ((Association) object).impl == impl) {
                return true;
            } else if (((Association) object).key.equals(key)
                    && ((Association) object).impl.equals(impl)) {
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