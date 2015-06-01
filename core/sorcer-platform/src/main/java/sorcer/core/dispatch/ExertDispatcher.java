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

package sorcer.core.dispatch;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.lease.LeaseRenewalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.Tuple2;
import sorcer.core.DispatchResult;
import sorcer.core.Dispatcher;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.Mograms;
import sorcer.core.monitor.MonitorUtil;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.Provider;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import javax.security.auth.Subject;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.*;
import static sorcer.service.Exec.*;

@SuppressWarnings("rawtypes")
abstract public class ExertDispatcher implements Dispatcher {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ServiceExertion xrt;

    protected ServiceExertion masterXrt;

    protected List<Mogram> inputXrts;

	protected volatile int state = Exec.INITIAL;

    protected boolean isMonitored;

    protected Set<Context> sharedContexts;

    // All dispatchers spawned by this one.
    protected List<Uuid> runningExertionIDs = Collections.synchronizedList(new LinkedList<Uuid>());

    // subject for whom this dispatcher is running.
    // make sure subject is set before and after any object goes out and comes
    // in dispatcher.
    protected Subject subject;

    protected Provider provider;

    protected static Map<Uuid, Dispatcher> dispatchers
            = new HashMap<Uuid, Dispatcher>();

	protected ThreadGroup disatchGroup;
    protected ProvisionManager provisionManager;

    public static Map<Uuid, Dispatcher> getDispatchers() {
		return dispatchers;
	}

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }


    private LeaseRenewalManager lrm = null;

	public ExertDispatcher(Exertion exertion,
                           Set<Context> sharedContexts,
                           boolean isSpawned,
                           Provider provider,
                           ProvisionManager provisionManager) {
        ServiceExertion sxrt = (ServiceExertion)exertion;
		this.xrt = sxrt;
        this.subject = sxrt.getSubject();
        this.sharedContexts = sharedContexts;
        this.isMonitored = sxrt.isMonitorable();
        this.provider = provider;
        this.provisionManager = provisionManager;
    }

    public void exec() {
        dispatchers.put(xrt.getId(), this);
        state = Exec.RUNNING;
        xrt.setStatus(state);
        if (xrt instanceof Job) {
            masterXrt = (ServiceExertion) ((Job) xrt).getMasterExertion();
        }
        try {
            beforeParent(xrt);
            doExec();
            afterExec(xrt);
            xrt.finalizeOutDataContext();
        } catch (Exception e) {
            logger.warn("Exertion dispatcher thread killed by exception: {}", e.getMessage());
            xrt.setStatus(Exec.FAILED);
            state = Exec.FAILED;
            xrt.reportException(e);
        } finally {
            try {
                MonitoringSession monSession = MonitorUtil.getMonitoringSession(xrt);
                if (lrm!=null && monSession!=null) lrm.remove(monSession.getLease());
            } catch (Exception ce) {
                logger.warn("Problem removing lease for : " + xrt.getName() + " " + Exec.State.name(xrt.getStatus()) , ce);
            }
            dispatchers.remove(xrt.getId());
        }
    }

    abstract protected void doExec() throws SignatureException, ExertionException, RemoteException, MogramException;
    abstract protected List<Mogram> getInputExertions() throws ContextException;

    protected void beforeParent(Exertion exertion) throws ContextException, ExertionException {
        logger.debug("before parent {}", exertion);
        reconcileInputExertions(exertion);
        updateInputs(exertion);
        checkProvision();
        inputXrts = getInputExertions();
    }

    protected void beforeExec(Exertion exertion) throws ExertionException, SignatureException {
        logger.debug("before exert {}", exertion);
        try {
            // Provider is expecting exertion to be in context
            exertion.getContext().setExertion(exertion);
            updateInputs(exertion);
        } catch (ContextException e) {
            throw new ExertionException(e);
        }
        // If Job, new dispatcher will update inputs for it's Exertion
        // in catalog dispatchers, if it is a job, then new dispatcher is
        // spawned
        // and the shared contexts are passed. So the new dispatcher will update
        // inputs
        // of tasks inside the jobExertion. But in space, all inputs to a new
        // job are
        // to be updated before dropping.
        try {
            exertion.getControlContext().appendTrace(provider.getProviderName()
                    + " dispatcher: " + getClass().getName());
        } catch (RemoteException e) {
            logger.warn("Exception on local call", e);
        }
        ((ServiceExertion) exertion).startExecTime();
        exertion.setStatus(Exec.RUNNING);

    }

    protected void afterExec(Exertion result) throws ContextException, ExertionException {
        logger.debug("After exert {}", result);
    }

    @Override
    public DispatchResult getResult() {
        /**
         * The default implementation - wait for status to be changed by another thread
         */

        try {
            while (!finished())
                Thread.sleep(50);
        } catch (InterruptedException e) {
            logger.warn("Interrupted!", e);
        }
        return new DispatchResult(State.values()[state], xrt);
    }

    private boolean finished(){
        return state == State.DONE.ordinal() || state == State.FAILED.ordinal();
    }

    /**
     * If the {@code Exertion} is provisionable, deploy services.
     *
     * @throws ExertionException if there are issues dispatching the {@code Exertion}
     */
    protected void checkProvision() throws ExertionException {
        if(xrt.isProvisionable() && xrt.getDeployments().size()>0) {
            try {
                provisionManager.deployServices();
            } catch (DispatcherException e) {
            	logger.warn("Unable to deploy services", e);
                throw new ExertionException("Unable to deploy services", e);
            }
        }
    }

    public Exertion getExertion() {
        return xrt;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    protected class CollectResultThread implements Runnable {
        public void run() {
            xrt.startExecTime();
            try {
                collectResults();
                xrt.setStatus(DONE);
            } catch (Exception ex) {
                xrt.setStatus(FAILED);
                xrt.reportException(ex);
                ex.printStackTrace();
            }
            if (xrt.isExecTimeRequested())
                xrt.stopExecTime();
            dispatchers.remove(xrt.getId());
        }
    }

    protected void collectResults() throws ExertionException, SignatureException{}
    //protected abstract void dispatchExertions() throws ExertionException, SignatureException;

    protected void collectOutputs(Mogram mo) throws ContextException {
        if (sharedContexts == null) {
            logger.warn("Trying to update sharedContexts but it is null for exertion: " + mo);
            return;
        }
        if (mo instanceof Exertion) {
            List<Context> contexts = Mograms.getTaskContexts((Exertion) mo);
            logger.debug("Contexts to check if shared: " + contexts.toString());
            for (Context ctx : contexts) {
                if (((ServiceContext) ctx).isShared()) {
                    sharedContexts.add(ctx);
                    logger.debug("Added exertion shared context: " + ctx);
                }
            }
        } else if (mo instanceof Model) {
            try {
                sharedContexts.add((Context) ((Model)mo).getResponse());
                logger.debug("Added model shared context: " + mo);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }

//      for (int i = 0; i < contexts.size(); i++) {
//			if (!sharedContexts.contains(contexts.get(i)))
//				sharedContexts.add(contexts.get(i));
//            if (((ServiceContext)contexts.get(i)).isShared())
//                sharedContexts.add(contexts.get(i));
//        }
    }

    protected void updateInputs(Exertion ex) throws ExertionException, ContextException {
        logger.debug("updating inputs for {}", ex.getName());
        List<Context> inputContexts = Mograms.getTaskContexts(ex);
        for (Context inputContext : inputContexts)
            updateInputs((ServiceContext) inputContext);
    }

	protected void updateInputs(ServiceContext toContext)
			throws ExertionException {
		ServiceContext fromContext;
		String toPath = null, newToPath = null, toPathcp, fromPath = null;
		int argIndex = -1;
		try {
			Map<String, String> toInMap = Contexts.getInPathsMap(toContext);
			if (toInMap.size()>0) {
                logger.debug("updating inputs in context toContext = {}", toContext);
                logger.debug("updating based on = {}", toInMap);
            }
			for (Map.Entry<String, String> e  : toInMap.entrySet()) {
                toPath = e.getKey();
				// find argument for parametric context
				if (toPath.endsWith("]")) {
					Tuple2<String, Integer> pair = getPathIndex(toPath);
					argIndex = pair._2;
					if	(argIndex >=0) {
						newToPath = pair._1;
					}
				}
				toPathcp = e.getValue();
				logger.debug("toPathcp = {}", toPathcp);
				fromPath = Contexts.getContextParameterPath(toPathcp);
                String ctxId = Contexts.getContextParameterID(toPathcp);
				if (ctxId.length()>0) logger.debug("context ID = {}", ctxId);
				fromContext = getSharedContext(fromPath, ctxId);
				logger.debug("fromContext = {}", fromContext);
				logger.debug("before updating toContext: {}", toContext
                        + "\n>>> TO path: " + toPath + "\nfromContext: "
                        + fromContext + "\n>>> FROM path: " + fromPath);
                if (fromContext != null) {
                    // make parametric substitution if needed
                    if (argIndex >=0 ) {
                        Object args = toContext.getValue(Context.PARAMETER_VALUES);
                        if (args.getClass().isArray()) {
                            if (Array.getLength(args) > 0) {
                                Array.set(args, argIndex, fromContext.getValue(fromPath));
                            } else {
                                // the parameter array is empty
								Object[] newArgs;
                                newArgs = new Object[] { fromContext.getValue(fromPath) };
                                toContext.putValue(newToPath, newArgs);
                            }
                        }
                    } else {
                        // make contextual substitution
                        Contexts.copyValue(fromContext, fromPath, toContext, toPath);
                    }
					logger.debug("updated dataContext:\n" + toContext);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
			throw new ExertionException("Failed to update data dataContext: " + toContext.getName()
                    + " at: " + toPath + " from: " + fromPath + "\n" + ex.getMessage(), ex);
        }
    }

    private Tuple2<String, Integer> getPathIndex(String path) {
        int index = -1;
        String newPath = null;
        int i1 = path.lastIndexOf('/');
        String lastAttribute = path.substring(i1+1);
        if (lastAttribute.charAt(0) == '[' && lastAttribute.charAt(lastAttribute.length() - 1) == ']') {
            index = Integer.parseInt(lastAttribute.substring(1, lastAttribute.length()-1));
            newPath = path.substring(0, i1+1);
        }
        return new Tuple2<String, Integer>(newPath, index);
    }

    protected ServiceContext getSharedContext(String path, String id) {
		// try to get the dataContext with particular id.
		// If not found, then find a dataContext with particular path.
		if (Context.EMPTY_LEAF.equals(path) || "".equals(path))
            return null;
        synchronized (sharedContexts) {
            if (id != null && id.length() > 0) {
                for (Context hc : sharedContexts) {
                    Uuid sharedCtxId = UuidFactory.create(id);
                    logger.debug("Comparing: " + sharedCtxId + " with: " + hc.getId() + "\n" + hc);
                    if (sharedCtxId.equals(hc.getId()))
                        return (ServiceContext) hc;
                }
            } else {
                for (Context hc : sharedContexts) {
                    if (hc.containsPath(path))
                        return (ServiceContext) hc;
                }
            }
        }
        return null;
    }

    public boolean isMonitorable() {
        return isMonitored;
    }

    protected void reconcileInputExertions(Mogram mo) throws ContextException {
        if (mo.getStatus() == DONE) {
            collectOutputs(mo);
            if (inputXrts != null)
                inputXrts.remove(mo);
        } else {
            mo.setStatus(INITIAL);
            if (mo instanceof CompoundExertion) {
                CompoundExertion ce = (CompoundExertion) mo;
                for (Mogram sub : ce.getMograms())
                    reconcileInputExertions(sub);
            }
        }
    }

    public LeaseRenewalManager getLrm() {
        return lrm;
    }

    public void setLrm(LeaseRenewalManager lrm) {
        this.lrm = lrm;
    }
}
