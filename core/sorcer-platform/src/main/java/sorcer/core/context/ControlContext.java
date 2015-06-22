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

package sorcer.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.monitor.MonitoringManagement;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.*;
import sorcer.service.Signature.Kind;
import sorcer.util.Stopwatch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ControlContext extends ServiceContext<Object> implements StrategyContext {

	private static final long serialVersionUID = 7280700425027799253L;

	// job broker
	public final static String JOBBER_IS_DIRECT = "jobber" + CPS + "direct";

	// control context name
	public final static String CONTROL_CONTEXT = "control/strategy";

	/**
	 * A flow type indicates if this exertion can be executed sequentially, in
	 * parallel, or concurrently with other component exertions within this
	 * exertion. The concurrent execution requires all mapped inputs in the
	 * exertion context to be assigned before this exertion can be executed.
	 */
	public final static String EXERTION_FLOW = "exertion" + CPS + "flow";

	public final static String EXERTION_PROVISIONABLE = "exertion" + CPS
			+ "provisionable";

	public final static String SHELL_REMOTE = "shell" + CPS
			+ "remote";

	public final static String EXERTION_OPTI = "exertion" + CPS + "opti";

	public final static String EXEC_STATE = "exertion" + CPS + "exec" + CPS
			+ "state";

	// exertion monitor state
	public final static String EXERTION_MONITORABLE = "exertion" + CPS
			+ "monitorable";

	// exertion access type
	public final static String EXERTION_ACCESS = "exertion" + CPS + "access";

	// final static String JOB_NO_WAIT = "job/no wait";
	public final static String NODE_REFERENCE_PRESERVED = "job" + CPS + "node"
			+ CPS + "reference" + CPS + "preserved";

	// a rendezovous provider is either Jobber or Spacer
	public final static String RENDEZVOUS_NAME = "rendezvous" + CPS + "name";

	public final static String NOTIFY_EXEC = "notify" + CPS + "execution" + CPS
			+ "to:";

	public final static String EXERTION_COMMENTS = "exertion" + CPS
			+ "comments";

	public final static String EXERTION_FEEDBACK = "exertion" + CPS
			+ "feedback";

	public final static String GET_EXEC_TIME = "get" + CPS + "exec/time";

	public final static String EXERTION_REVIEW = "exertion" + CPS + "review";

	public final static String EXEC_TIME = "exec" + CPS + "time";

	public final static String PRIORITY = "priority";

	public final static String MASTER_EXERTION = "exertion" + CPS + "master"
			+ CPS + "exertion";

	public final static String EXERTION = "controlled/exertion";

	public final static String ID = "/id";

	public final static String SKIPPED = "skipped";

	public final static String SKIPPED_ = "skipped" + CPS;

	// .rerun is an attribute used as buffer by client to store information
	// about what all
	// exertions are to be rerun. Stored as an attribute in control context of
	// job / jobExertions
	// It is removed before the job is submitted for running.
	public final static String RERUN = "RERUN";

	// control context values
	public final static String PULL = "pull";

	public final static String QOS_PULL = "qos-pull";

	public final static String PUSH = "push";

	public final static String QOS_PUSH = "qos-push";

	public final static String SWIF = "swif";

	public final static String DIRECT = "direct";

    public final static String AUTO = "auto";

	public final static String PARALLEL = "parallel";

	public final static String SEQUENTIAL = "sequential";

	public final static String WAIT = "wait";

	public final static String NOTIFY = "notify";

	public final static String NOTIFY_ALL = "notifyAll";

	public final static String SORCER_VARIABLES_PATH = "supportObjects/sorcerVariables";

	public final static String EXERTION_MONITORING = "exertion/monitor/enabled";

	public final static String EXERTION_WAITABLE = "exertion/waitable";

	public final static String EXERTION_WAITED_FROM = "exertion/waited/from";

	public final static String NOTIFICATION_MANAGEMENT = "exertion/notifications/enabled";

	public final static String TRUE = "true";

	public final static String FALSE = "false";

	public final static String TRACE_LIST = "exertion/exec/trace";

	public final static String EXERTION_TRACABLE = "exertion/tracable";

	private List<ThrowableTrace> exceptions = new ArrayList<ThrowableTrace>();

	private List<Signature> signatures = new ArrayList<Signature>();

	private List<String> traceList = new ArrayList<String>();

	private Object mutexId;

	// for getting execution time
	private Stopwatch stopwatch;

	// this class logger
	private static Logger logger = LoggerFactory.getLogger(ControlContext.class.getName());

	public ControlContext() {
		super(CONTROL_CONTEXT, CONTROL_CONTEXT);
		setDomainId("0");
		setSubdomainId("0");
		setExecTimeRequested(true);
        // Changed for Sorter
		setFlowType(Flow.AUTO);
        //setFlowType(Flow.SEQ);

		setAccessType(Access.PUSH);
		setExecState(Exec.State.INITIAL);
		setComponentAttribute(GET_EXEC_TIME);
		setComponentAttribute(SKIPPED_);
		setComponentAttribute(EXERTION_REVIEW);
		setComponentAttribute(PRIORITY);
		setComponentAttribute(NOTIFY_EXEC);
		setMonitorable(false);
		setProvisionable(false);
		setShellRemote(false);
		setNotifierEnabled(false);
		setExecTimeRequested(true);
		setWaitable(true);
		put(EXCEPTIONS, exceptions);
		put(TRACE_LIST, traceList);
		put(EXERTION_TRACABLE, false);
	}

	public ControlContext(Exertion exertion) {
		this();
		subjectValue = exertion.getName();
		// make it visible via the path EXERTION
		try {
			Exertion erxt = (Exertion) getValue(EXERTION);
			if (exertion != null) {
				putValue(EXERTION, exertion);
                //
                if (exertion instanceof NetTask || exertion instanceof NetJob)
                    setProvisionable(true);

			}
		} catch (ContextException e) {
			e.printStackTrace();
		}
	}

	public void setMasterExertion(Exertion e) {
		put(MASTER_EXERTION, ((ServiceExertion) e).getId());
	}

	public boolean isSequential() {
		return SEQUENTIAL.equals(get(EXERTION_FLOW));
	}

	public boolean isParallel() {
		return PARALLEL.equals(get(EXERTION_FLOW));
	}

	public boolean isExertionMaster(Exertion exertion) throws ContextException {
		return (exertion != null && exertion.getContext().getName()
				.equals(get(MASTER_EXERTION)));
	}

	public boolean isWaitable() {
		return Boolean.TRUE.equals(get(EXERTION_WAITABLE));
	}

	public void setWaitable(boolean state) {
		put(EXERTION_WAITABLE, new Boolean(state));
	}

	public void isWait(Wait value) {
		if (Wait.YES.equals(value) || Wait.TRUE.equals(value))
			put(EXERTION_WAITABLE, true);
		else if (Wait.NO.equals(value) || Wait.FALSE.equals(value))
			put(EXERTION_WAITABLE, false);
	}

	public void setTracable(boolean isTracable) {
		if (isTracable)
			put(EXERTION_TRACABLE, true);
		else
			put(EXERTION_TRACABLE, false);
	}

	public Boolean isTracable() throws ContextException {
		return (Boolean)getValue(EXERTION_TRACABLE);
	}

	public void setNotifierEnabled(boolean state) {
		put(NOTIFICATION_MANAGEMENT, new Boolean(state));
	}

	public boolean isNotifierEnabled() {
		return Boolean.TRUE.equals(get(NOTIFICATION_MANAGEMENT));
	}

	public void setNodeReferencePreserved(boolean state) {
		put(NODE_REFERENCE_PRESERVED, new Boolean(state));
	}

	public boolean isNodeReferencePreserved() {
		return Boolean.TRUE.equals(get(NODE_REFERENCE_PRESERVED));
	}

	public Flow getFlowType() {
		return (Flow) get(EXERTION_FLOW);
	}

	public void setFlowType(Flow type) {
		put(EXERTION_FLOW, type);
	}

	public boolean isMonitorable() {
		return Boolean.TRUE.equals(get(EXERTION_MONITORABLE));
	}

	public void isMonitorable(Monitor value) {
		if (Monitor.YES.equals(value) || Monitor.TRUE.equals(value))
			put(EXERTION_MONITORABLE, true);
		else if (Monitor.NO.equals(value) || Monitor.FALSE.equals(value))
			put(EXERTION_MONITORABLE, false);
	}

	public void setMonitorable(boolean state) {
		put(EXERTION_MONITORABLE, new Boolean(state));
	}

	public boolean isProvisionable() {
		return Boolean.TRUE.equals(get(EXERTION_PROVISIONABLE));
	}

	public void setProvisionable(boolean state) {
		put(EXERTION_PROVISIONABLE, new Boolean(state));
	}

	public boolean isShellRemote() {
		return Boolean.TRUE.equals(get(SHELL_REMOTE));
	}

	public void setShellRemote(boolean state) {
		put(SHELL_REMOTE, new Boolean(state));
	}

	public void setOpti(Opti optiType) {
		put(EXERTION_OPTI, optiType);
	}

	public Opti getOpti() {
		return (Opti) get(EXERTION_OPTI);
	}

	public void setExecState(Exec.State state) {
		put(EXEC_STATE, state);
	}

	public Exec.State getExecState() {
		return (Exec.State) get(EXEC_STATE);
	}

	public MonitoringManagement getMonitor() {
		return (MonitoringManagement) get(EXERTION_MONITORING);
	}

	public void setMonitor(MonitoringManagement monitor) {
		put(EXERTION_MONITORING, monitor);
	}

	public void setAccessType(Access access) {
		if (Access.PULL.equals(access) || Access.PUSH.equals(access)
				|| Access.SWIF.equals(access) || Access.DIRECT.equals(access))
			put(EXERTION_ACCESS, access);
	}

	public Access getAccessType() {
		return (Access) get(EXERTION_ACCESS);
	}

	public void setExertionComments(String message) {
		if (message == null || message.trim().length() == 0)
			remove(EXERTION_COMMENTS);
		else
			put(EXERTION_COMMENTS, message);
	}

	public String getExertionComments() {
		return (String) get(EXERTION_COMMENTS);
	}

	public void setExecTimeRequested(boolean state) {
		if (!state)
			remove(GET_EXEC_TIME);
		else
			put(GET_EXEC_TIME, new Boolean(state));
	}

	public boolean isExecTimeRequested() {
		return Boolean.TRUE.equals(get(GET_EXEC_TIME));
	}

	public void setNotifyList(String list) {
		if (list == null || list.trim().length() == 0)
			remove(NOTIFY_EXEC);
		else
			put(NOTIFY_EXEC, list);
	}

	public String getNotifyList() {
		return (String) get(NOTIFY_EXEC);
	}

	public void setRendezvousName(String rendezvous) {
		if (rendezvous == null || rendezvous.trim().length() == 0)
			remove(RENDEZVOUS_NAME);
		else
			put(RENDEZVOUS_NAME, rendezvous);
	}

	public String getRendezvousName() {
		return (String) get(RENDEZVOUS_NAME);
	}

	public void setFeedback(String message) {
		put(EXERTION_COMMENTS, message);
	}

	public String getFeedback() {
		return (String) get(EXERTION_COMMENTS);
	}

	public void setExecTimeRequested(Exertion exertion, boolean b) {
		if (b)
			addAttributeValue(exertion, GET_EXEC_TIME, TRUE);
		else
			addAttributeValue(exertion, GET_EXEC_TIME, FALSE);
	}

	public void setSkipped(Exertion exertion, boolean b) {
		if (b)
			addAttributeValue(exertion, SKIPPED_, TRUE);
		else
			addAttributeValue(exertion, SKIPPED_, FALSE);
	}

	public boolean isSkipped(Exertion exertion) throws ContextException {
		boolean result;
		try {
			String b = getAttributeValue(exertion, SKIPPED_);
			result = TRUE.equals(b);
		} catch (java.lang.ClassCastException e) {
			throw new ContextException(e);
		}
		return result;
	}

	public void startExecTime() {
		if (stopwatch == null)
			stopwatch = new Stopwatch();
		stopwatch.start();
	}

	public void stopExecTime() {
		if (stopwatch == null) {
			logger.info("No stopwatch available for stopExecTime");
			return;
		}
		stopwatch.stop();
	}

	public String getExecTime() {
		if (stopwatch == null) {
			return "";
		} else
			return stopwatch.getTime();
	}

	public void setReview(Exertion ex, boolean b) {
        addAttributeValue(ex, EXERTION_REVIEW, Boolean.toString(b));
	}

	public boolean isReview(Exertion exertion) {
		String b = getAttributeValue(exertion, EXERTION_REVIEW);
		return TRUE.equals(b);
	}

	public void setPriority(Exertion exertion, int priorityValue) {
		addAttributeValue(exertion, PRIORITY, Integer.toString(priorityValue));
	}

	public int getPriority(Exertion exertion) {
		int result;
		try {
			String i = getAttributeValue(exertion, PRIORITY);
			result = (i == SorcerConstants.NULL) ? NORMAL_PRIORITY : Integer.parseInt(i);
		} catch (java.lang.ClassCastException ex) {
			logger.warn("getPriority", ex);
			return -1;
		}
		return result;
	}

	public void setNotifyList(Exertion exertion, String list) {
		if (list == null || list.trim().length() == 0)
			addAttributeValue(exertion, NOTIFY_EXEC, SorcerConstants.NULL);
		addAttributeValue(exertion, NOTIFY_EXEC, list);
	}

	public String getNotifyList(Exertion ex) {
		return getAttributeValue(ex, NOTIFY_EXEC);
	}

	public void registerExertion(Mogram mogram) throws ContextException {
		if (mogram instanceof Job)
			put(((Job)mogram).getControlContext().getName(),
					((ServiceExertion) mogram).getId());
		else if (mogram instanceof Exertion) {
			put(((Exertion) mogram).getContext().getName(), mogram.getId());
		} else {
			// TODO explain if registration is still needed
			put(mogram.getName(), mogram.getId());
			return;
		}
		setPriority((Exertion) mogram,
				MAX_PRIORITY - ((ServiceExertion) mogram).getIndex());
		setExecTimeRequested(((Exertion)mogram), true);
	}

	public void deregisterExertion(Mogram mogram, Mogram componentMogram)
			throws ContextException {
		CompoundExertion parent = (CompoundExertion)mogram;
		Exertion component = (Exertion)componentMogram;
		String path = component.getContext().getName();
		remove(path);
		for (int i = component.getIndex(); i < parent.size(); i++) {
			String oldPath = parent.get(i).getContext().getName();
			parent.get(i).setIndex(i);
			put(parent.get(i).getContext().getName(), remove(oldPath));
			Map map;
			Map<String, Map<String, String>> imc = getMetacontext();
			String key;
			Iterator keys = ((Map) imc.get(CONTEXT_ATTRIBUTES)).keySet().iterator();
			while (keys.hasNext()) {
				key = (String) keys.next();
				map = (Map) getMetacontext().get(key);
				if (map != null && map.size() > 0 && map.containsKey(oldPath))
					map.put(parent.get(i).getContext().getName(),
							map.remove(oldPath));
			}
		}
	}

	public Context addAttributeValue(String attributeName, String attributeValue)
			throws ContextException {
		return addComponentAssociation(EXERTION, attributeName, attributeValue);
	}

	private Context addAttributeValue(Exertion exertion, String attributeName,
			String attributeValue) {
		Context result = null;
		try {
			result = addComponentAssociation(exertion.getContext().getName(),
					attributeName, attributeValue);
		} catch (ContextException ex) {
			// I know this won't happen if called from this class
			// (because I know the calling attributes), hence private access
			ex.printStackTrace();
		}
		return result;
	}

	public Context addComponentAssociation(String path, String attributeName,
			String attributeValue) throws ContextException {
		if (!containsPath(path))
			put(path, SorcerConstants.NULL);
		return super.addComponentAssociation(path, attributeName,
				attributeValue);
	}

	public String getAttributeValue(Exertion exertion, String attributeName) {
		try {
			return getAttributeValue(exertion.getContext().getName(),
					attributeName);
		} catch (ContextException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public String getAttributeValue(String attributeName) {
		try {
			return getAttributeValue(EXERTION, attributeName);
		} catch (ContextException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public void updateExertionName(Exertion exertion) throws ContextException {
		String key, oldPath = null;
		Iterator e = keyIterator();
		while (e.hasNext()) {
			key = (String) e.next();
			if (key.endsWith("[" + ((CompoundExertion) exertion).getIndex()
					+ "]" + ID)) {
				oldPath = key;
				break;
			}
		}
		String newPath = exertion.getContext().getName();
		Map map;
		Map<String, Map<String, String>> imc = getMetacontext();
		e = ((Map) imc.get(CONTEXT_ATTRIBUTES)).keySet().iterator();
		while (e.hasNext()) {
			key = (String) e.next();
			map = (Map) imc.get(key);
			if (map != null && map.size() > 0 && map.containsKey(oldPath))
				map.put(newPath, map.remove(oldPath));
		}
	}

	public void appendTrace(String info) {
		try {
			if (isTracable())
                traceList.add(info);
		} catch (ContextException e) {
			// ignore it
		}
	}

	public void addException(ThrowableTrace et) {
		exceptions.add(et);
	}

	public void addException(Throwable t) {
		exceptions.add(new ThrowableTrace(t));
	}

	public void addException(String message, Throwable t) {
		exceptions.add(new ThrowableTrace(message, t));
	}

	public List<ThrowableTrace> getExceptions() {
		return exceptions;
	}

	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		if (t.getStackTrace() != null)
			t.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}

	public String describeExceptions() {
		if (exceptions.size() == 0)
			return "no exceptions thrown\n";

		StringBuilder sb = new StringBuilder();
		for (ThrowableTrace exceptionTrace : exceptions) {
			sb.append(exceptionTrace.stackTrace).append("\n");
		}
		return sb.toString();
	}

	public List<Signature> getSignatures() {
		return signatures;
	}

	public ServiceSignature getSignature(Kind kind) {
		for (Signature s : signatures) {
			if (((ServiceSignature) s).isKindOf(kind)) {
				return (ServiceSignature) s;
			}
		}
		return null;
	}

	public void setSignatures(List<Signature> signatures) {
		this.signatures = signatures;
	}

	public List<String> getTrace() {
		return traceList;
	}

	public Stopwatch getStopwatch() {
		return stopwatch;
	}

	public void setStopwatch(Stopwatch stopwatch) {
		this.stopwatch = stopwatch;
	}

	public Object getMutexId() {
		return mutexId;
	}

	public void setMutexId(Object mutexId) {
		this.mutexId = mutexId;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if (ServiceExertion.debug) {
			sb.append("\nControl Context Exceptions: \n");
			sb.append(describeExceptions());
		}
		return sb.toString();
	}

}
