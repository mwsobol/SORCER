/*
 * Distribution Statement
 * 
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 * 
 * Disclaimer
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
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
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("rawtypes")
public class ContextSelector {
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

	private String selectedTaskName;

	public ContextSelector(String path) {
		name = defaultName + count++;
		selectedPath = path;
		this.paths.add(path);
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
		selectedTaskName = task.getName();
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

	public Object doSelection(Object input) throws ContextException {
		Object in = input;
		if (input != null && in instanceof Context) {
			target = in;
		} else
			throw new ContextException("ContextSelector requires input of Context type.");

		try {
			if (selectedTaskName != null) {
				target = ((ServiceContext)in).getTaskContext(selectedTaskName);
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
			Iterator e = ((ServiceContext)out).keyIterator();
			while (e.hasNext()) {
				String p = (String) e.next();
				return out.getValue(p);
			}
			return null;
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
	 * @return the selectedTaskName
	 */
	public String getTaskName() {
		return selectedTaskName;
	}

	/**
	 * <p>
	 * Assigns a task name of this filter direct target responseContext.
	 * </p>
	 * 
	 * @param selectedTaskName
	 *            the selectedTaskName to set
	 */
	public void setTaskName(String selectedTaskName) {
		this.selectedTaskName = selectedTaskName;
	}
}
