/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.context;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Positioning;
import sorcer.service.Signature;

import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class PositionalContext<T> extends ServiceContext<T> implements
		Positioning {

	private static final long serialVersionUID = -8607789835474515562L;
	protected int tally = 0;
	
	public PositionalContext() {
		super();
	}
	
	public PositionalContext(String name) {
		super(name);
	}

	public PositionalContext(String name, Signature builder) {
		super(name, builder);
	}

    public PositionalContext(Context context) throws ContextException {
        super(context);
    }
    
	public PositionalContext(String name, String subjectPath, Object subjectValue) {
		super(name, subjectPath, subjectValue);
	}

    public PositionalContext getSubcontext(String... paths) throws ContextException {
        // bare-bones subcontext
        PositionalContext subcntxt = new PositionalContext();
        subcntxt.setSubject(subjectPath, subjectValue);
        subcntxt.setName(getName() + "-subcontext");
        subcntxt.setDomainId(getDomainId());
        subcntxt.setSubdomainId(getSubdomainId());
        if  (paths != null && paths.length > 0) {
            for (int i = 0; i < paths.length; i++)
                subcntxt.putInoutValueAt(paths[i], getValue(paths[i]), tally + 1);
        }
        return subcntxt;
    }

    public ServiceContext getEvaluatedInSubcontext(String... paths) throws ContextException {
        PositionalContext subcntxt = (PositionalContext) getSubcontext();
        List<String>  ips = getInPaths();
        for (String p : paths) {
            for (int i = 1; i <= tally; i++) {
                if (ips.contains(p)) {
                    subcntxt.putInValueAt(p, getValue(p), i);
                    break;
                }
            }
        }
		subcntxt.getData().putAll(getInEntContext().getData());
        return subcntxt;
    }

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#getInValueAt(sorcer.service.Context, int)
	 */
	@Override
	public Object getInValueAt(int index) throws ContextException {
		return Contexts.getInValueAt(this, index);
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#getInoutValueAt(sorcer.service.Context, int)
	 */
	@Override
	public Object getInoutValueAt(int index) throws ContextException {
		return Contexts.getInoutValueAt(this, index);
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#getOutValueAt(sorcer.service.Context, int)
	 */
	@Override
	public Object getOutValueAt(int index) throws ContextException {
		return Contexts.getOutValueAt(this, index);

	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#getValueAt(sorcer.service.Context, int)
	 */
	@Override
	public Object getValueAt(int index) throws ContextException {
		return Contexts.getValueAt(this, index);

	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#getValuesAt(sorcer.service.Context, int)
	 */
	@Override
	public List<Object> getValuesAt(int index) throws ContextException {
		Object[] objs = Contexts.getValuesAt(this, index);
		return Arrays.asList(objs);
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#putInValueAt(sorcer.service.Context, java.lang.String, java.lang.Object, int)
	 */
	@Override
	public Object putInValueAt(String path, Object value,
			int index) throws ContextException {
		super.putValue(path, (T)value);
		if (index >= tally) tally++;
		mark(path, Context.CONTEXT_PARAMETER + APS + Context.DA_IN + APS + APS + APS);
		mark(path, Context.OPP + APS + Context.DA_IN + APS + index);
		return value;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#putInoutValueAt(sorcer.service.Context, java.lang.String, java.lang.Object, int)
	 */
	@Override
	public Object putInoutValueAt(String path, Object value,
			int index) throws ContextException {
		super.putValue(path, (T)value);
		if (index >= tally) tally++;
		mark(path, Context.CONTEXT_PARAMETER + APS + Context.DA_INOUT + APS + APS + APS);
		mark(path, Context.OPP + APS + Context.DA_INOUT + APS + index);
		return value;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.Positioning#putOutValueAt(sorcer.service.Context, java.lang.String, java.lang.Object, int)
	 */
	@Override
	public Object putOutValueAt(String path, Object value, int index)
			throws ContextException {
		return putOutValueAt(path, value, null, index);
	}

	public Object putOutValueAt(String path, Object value, Class valType, int index)
			throws ContextException {
		super.putValue(path, (T)value);
		if (index >= tally) tally++;
		if (valType != null)
			mark(path, Context.CONTEXT_PARAMETER + APS + Context.DA_OUT + APS + APS + APS + valType);
		else
			mark(path, Context.CONTEXT_PARAMETER + APS + Context.DA_OUT + APS + APS + APS);

		mark(path, Context.OPP + APS + Context.DA_OUT + APS + index);
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.context.Positioning#putValueAt(sorcer.service.Context,
	 * java.lang.String, java.lang.Object, int)
	 */
	@Override
	public Object putValueAt(final String path, Object value, int index) throws ContextException {
		super.putValue(path, (T)value);
		if (index >= tally) tally++;
		mark(path, Context.INDEX + APS + index);
		return value;
	}

	@Override
	public T putValue(String path, Object value) throws ContextException {
		if (this.containsPath(path)) {
			return super.putValue(path, (T)value);
		}
		else {
			int index = tally++;
			mark(path, Context.INDEX + APS + index);
			return (T)putValueAt(path, value, index);
		}
	}
	
	public int getTally() {
		return tally;
	}
}
