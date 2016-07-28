/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.SetterException;
import sorcer.service.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Mike Sobolewski
 */
public class ContextSelector implements ContextSelection {
	static final long serialVersionUID = -1L;

	protected static int count = 0;

	// name of this filter
	protected String name;

	final protected static Logger logger = LoggerFactory.getLogger(ContextSelector.class
			.getName());

	private static String defaultName = "cxtSel-";

	private List<String> paths = new ArrayList<String>();

	private String selectedPath;

	protected Object target;

	private String selectedComponentName;

	public ContextSelector(String path) {
		name = defaultName + count++;
		selectedPath = path;
		this.paths.add(path);
	}

	public ContextSelector(List<String> paths) {
		name = defaultName + count++;
		this.paths = paths;
	}

	public ContextSelector(String selectorName, String path) {
		if (selectorName == null)
			name = defaultName + count++;
		else
			name = selectorName;
		selectedPath = path;
		this.paths.add(path);
	}

	public ContextSelector(Task task, String path) {
		name = defaultName + count++;
		selectedComponentName = task.getName();
		selectedPath = path;
		this.paths.add(path);
	}
	
	public void addPath(String path) {
		paths.add(path);
	}

	public void addPaths(String... paths) {
		for (String path : paths) {
			this.paths.add(path);
		}
	}
	
	public void addPaths(List<String> paths) {
		this.paths.addAll(paths);
	}

	public Object doSelect(Object input) throws ContextException {
		Object in = input;
		if (input != null && in instanceof Context) {
			target = in;
		} else
			throw new ContextException("ContextSelector requires input of Context type.");

		try {
			if (selectedComponentName != null) {
				target = ((ServiceContext)in).getTaskContext(selectedComponentName);
			} else {
				in = select((Context) target);
			}
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return in;
	}

	private Object select(Context in) throws ContextException {
		Object val;
		Context out = new ServiceContext("Selected Context");
		if (name != null)
			out.setSubject("filter" + SorcerConstants.CPS + name, new Date());

		if (selectedPath != null)
			return in.getValue(selectedPath);
		
		for (String path : paths) {
			val = in.getValue(path);
			if (val != null)
				out.putValue(path, val);
		}
		if (out.size() == 0) {
			return null;
		} else if (out.size() == 1) {
            return out.getValue(paths.get(0));
		} else
			return out;
	}

	public String setPath(String path) {
		selectedPath = path;
		return path;
	}
	
	public String setPath(int index) throws ContextException {
		String sp = paths.get(index);
		if (sp == null)
			throw new ContextException("unknown path in this filter at: " + index);
		selectedPath = sp;
		return sp;
	}
	

	public void setValue(Object value) throws SetterException {
		if (paths.size() == 1) {
			try {
				((Context) target).putValue(paths.get(0), value);
			} catch (ContextException e) {
				throw new SetterException(e);
			}
		} else
			throw new SetterException(
					"No unique responseContext path is vavailable for this filter.");
	}

	public String info() {
		return "ContextSelector for:" + paths;
	}


	public void setValue(String selector, Object value) throws SetterException {
		try {
			if (target == null)
				target = new ServiceContext();
			((Context) target).putValue(selector, value);
		} catch (ContextException e) {
			throw new SetterException(e);
		}

	}
	
	/**
	 * <p>
	 * Returns a task name of this selector direct target responseContext. The task responseContext is
	 * subcontext of the input (indirect) job responseContext.
	 * </p>
	 * 
	 * @return the selectedComponentName
	 */
	public String getComponentName() {
		return selectedComponentName;
	}

	/**
	 * <p>
	 * Assigns a task name of this filter direct target responseContext.
	 * </p>
	 * 
	 * @param selectedTaskName
	 *            the selectedComponentName to setValue
	 */
	public void setComponentName(String selectedTaskName) {
		this.selectedComponentName = selectedTaskName;
	}

    @Override
    public String getName() {
        return name;
    }
}
