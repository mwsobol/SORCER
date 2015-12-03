/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.dispatch;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.entry.UnusableEntriesException;
import net.jini.id.Uuid;
import net.jini.space.JavaSpace05;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.exertion.Mograms;
import sorcer.core.exertion.NetJob;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.monitor.MonitorUtil;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.Provider;
import sorcer.core.provider.SpaceTaker;
import sorcer.service.*;
import sorcer.service.space.SpaceAccessor;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static sorcer.service.Exec.*;
import static sorcer.util.StringUtils.tName;

public class SpaceParallelDispatcher extends ExertDispatcher {

    protected JavaSpace05 space;
    private int doneExertionIndex = 0;
    protected LokiMemberUtil loki;

    public SpaceParallelDispatcher(Exertion exertion,
           Set<Context> sharedContexts,
           boolean isSpawned,
           LokiMemberUtil loki,
           Provider provider,
           ProvisionManager provisionManager) throws ExertionException, ContextException {
        super(exertion, sharedContexts, isSpawned, provider, provisionManager);

        space = SpaceAccessor.getSpace();
        if (space == null) {
            throw new ExertionException("NO exertion space available!");
        }

        disatchGroup = new ThreadGroup("exertion-" + exertion.getId());
        disatchGroup.setDaemon(true);
        disatchGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);

        this.loki = loki;
	}

    public int getDoneExertionIndex() {
        return doneExertionIndex;
    }

    @Override
    protected List<Mogram> getInputExertions() throws ContextException {
        if (xrt instanceof Job)
            return Mograms.getInputExertions((Job) xrt);
        else if (xrt instanceof Block)
            return xrt.getAllMograms();
        else
            return null;
    }

    @Override
    public void doExec() throws SignatureException, ExertionException {
        new Thread(disatchGroup, new CollectResultThread(), tName("collect-" + xrt.getName())).start();

        for (Mogram mogram : inputXrts) {
            logger.info("Calling monSession.init from SpaceParallelDispatcher for: " + mogram.getName());
            MonitoringSession monSession = MonitorUtil.getMonitoringSession((Exertion)mogram);
            if (xrt.isMonitorable() && monSession!=null) {
                try {
                    if (monSession.getState()==State.INITIAL.ordinal()) {
                        logger.info("initializing monitoring from SpaceParallelDispatcher for " + mogram.getName());
                        monSession.init(ExertionDispatcherFactory.LEASE_RENEWAL_PERIOD, ExertionDispatcherFactory.DEFAULT_TIMEOUT_PERIOD);
                    }
                } catch (MonitorException me) {
                    logger.error("Problem starting monitoring for " + xrt.getName() + " " + me.getMessage());
                } catch (RemoteException re) {
                    logger.error("Problem starting monitoring for " + xrt.getName() + " " + re.getMessage());
                }
            }
            dispatchExertion((Exertion)mogram);
            try {
                afterExec((Exertion)mogram);
            } catch (ContextException ce) {
                logger.warn("Problem sending state to monitor");
            }
        }
	}

    protected void dispatchExertion(Exertion exertion) throws ExertionException, SignatureException {
        logger.debug("exertion #{}: exertion: {}", exertion.getIndex(), exertion);
        try {
            writeEnvelop(exertion);
            logger.debug("generateTasks ==> SPACE EXECUTE EXERTION: "
                    + exertion.getName());
            xrt.setStatus(INSPACE);
        } catch (RemoteException re) {
			logger.warn("Space not reachable....resetting space", re);
			space = SpaceAccessor.getSpace();
			if (space == null) {
				xrt.setStatus(FAILED);
				throw new ExertionException("NO exertion space available!");
			}
			if (masterXrt != null) {
				try {
					writeEnvelop(masterXrt);
				} catch (Exception e) {
					e.printStackTrace();
					xrt.setStatus(FAILED);
					throw new ExertionException(
							"Wrting master exertion into exertion space failed!",
							e);
				}
			}
		}
	}

	public void collectResults() throws ExertionException, SignatureException, RemoteException {
		int count = 0;
		// get all children of the underlying parent job
        List<ExertionEnvelop> templates = Arrays.asList(getTemplate(DONE), getTemplate(FAILED), getTemplate(ERROR));
        while(count < inputXrts.size() && state != FAILED) {
            Collection<ExertionEnvelop> results;
            try {
                results = space.take(templates, null, SpaceTaker.SPACE_TIMEOUT, Integer.MAX_VALUE);
                if (results.isEmpty())
                    continue;
                logger.debug("Got from space: " + results.size());
                for (ExertionEnvelop eee : results) {
                    logger.debug("Got: " + eee.toString());
                    logger.debug("Got: " + eee.exertion);
                }
                count += results.size();
            } catch (UnusableEntriesException e) {
                xrt.setStatus(FAILED);
                state = FAILED;
                Collection<UnusableEntryException> exceptions = e.getUnusableEntryExceptions();
                for (UnusableEntryException throwable : exceptions) {
                    logger.warn("UnusableEntryException! unusable fields = " + throwable.partialEntry, throwable);
                }
                cleanRemainingFailedExertions(xrt.getId());

                throw new ExertionException(e);
            } catch (Exception e) {
                xrt.setStatus(FAILED);
                state = FAILED;
                throw new ExertionException("Taking exertion envelop failed", e);
            } finally {
                synchronized (this) {
                    notify();
                }
            }
            handleResult(results);
        }

        if(xrt.getStatus()!=FAILED) {
            executeMasterExertion();
            state = DONE;
        }
        dispatchers.remove(xrt.getId());
    }

    protected ExertionEnvelop getTemplate(int state) {
        Uuid parentId = null;
        Uuid id = null;
        if(inputXrts.size()==1 && inputXrts.get(0)==xrt)
            id = xrt.getId();
        else
            parentId = xrt.getId();
        ExertionEnvelop tmpl = ExertionEnvelop.getTakeTemplate(parentId, id);
        tmpl.state = state;
        return tmpl;
    }

    protected void handleResult(Collection<ExertionEnvelop> results) throws ExertionException, SignatureException, RemoteException {
        boolean poisoned = false;
        for (ExertionEnvelop resultEnvelop : results) {

            logger.debug("HandleResult got result: " + resultEnvelop.describe());
            ServiceExertion input = (ServiceExertion) ((NetJob) xrt)
                    .get(resultEnvelop.exertion
                            .getIndex());
            ServiceExertion result = (ServiceExertion) resultEnvelop.exertion;
            int status = result.getStatus();
            if(status == DONE)
                postExecExertion(input, result);
            else if (status == FAILED) {
                if (!poisoned) {
                    addPoison(xrt);
                    poisoned = true;
                }
                handleError(result);
            }

            try {
                afterExec(result);
                //this.removeExertionListener(result.getId());
            } catch (ContextException ce) {
                logger.error("Problem sending status after exec to monitor");
            }
        }
    }

    protected void addPoison(Exertion exertion) {
        space = SpaceAccessor.getSpace();
        if (space == null) {
            return;
        }
        ExertionEnvelop ee = new ExertionEnvelop();
        if (exertion == xrt)
            ee.parentID = exertion.getId();
        else
            ee.parentID = exertion.getParentId();
        ee.state = Exec.POISONED;
        try {
            space.write(ee, null, Lease.FOREVER);
            logger.debug("written poisoned envelop for: "
                    + ee.describe() + "\n to: " + space);
        } catch (Exception e) {
            logger.warn("writting poisoned ExertionEnvelop", e);
        }
    }

    protected synchronized void changeDoneExertionIndex(int index) {
        logger.debug("[" + Thread.currentThread().getName() + "] - Updating changeDoneExertionIndex to: " + index+1);
        doneExertionIndex = index + 1;
        notifyAll();
    }

    // abstract in ExertionDispatcher
    protected void preExecExertion(Exertion exertion) throws ExertionException,
            SignatureException {
//		try {
//			exertion.getControlContext().appendTrace(provider.getProviderName()
//					+ " dispatcher: " + getClass().getName());
//		} catch (RemoteException e) {
//			// ignore it, local call
//		}
        try {
            updateInputs(exertion);
        } catch (ContextException e) {
            throw new ExertionException(e);
        }
        ((ServiceExertion) exertion).startExecTime();
        ((ServiceExertion) exertion).setStatus(RUNNING);
    }

/*    private void provisionProviderForExertion(Exertion exertion) {
        ProviderProvisionManager.provision(exertion, this);
    }*/

    protected void writeEnvelop(Exertion exertion) throws
            ExertionException, SignatureException, RemoteException {
        // setSubject before exertion is dropped
        space = SpaceAccessor.getSpace();
        if (space == null) {
            throw new ExertionException("NO exertion space available!");
        }

        /*if (exertion.isProvisionable())
            provisionProviderForExertion(exertion);*/

        ((ServiceExertion) exertion).setSubject(subject);
        preExecExertion(exertion);
        ExertionEnvelop ee = ExertionEnvelop.getTemplate(exertion);
        ee.state = INITIAL;
        try {
            space.write(ee, null, Lease.FOREVER);
            logger.debug("written envelop: "
                    + ee.describe() + "\n to: " + space);
        } catch (Exception e) {
            logger.warn("writeEnvelop", e);
            state = Exec.FAILED;
        }
    }

    protected ExertionEnvelop takeEnvelop(Entry template)
            throws ExertionException {
        space = SpaceAccessor.getSpace();
        if (space == null) {
            throw new ExertionException("NO exertion space available!");
        }
        ExertionEnvelop result;
        try {
            while (state == RUNNING) {
                result = (ExertionEnvelop) space.take(template, null, SpaceTaker.SPACE_TIMEOUT);
                if (result != null) {
                    return result;
                }
            }
            return null;
        } catch (UnusableEntryException e) {
            logger.warn("UnusableEntryException! unusable fields = " + e.partialEntry, e);
            throw new ExertionException("Taking exertion envelop failed", e);
        } catch (Throwable e) {
            throw new ExertionException("Taking exertion envelop failed", e);
        }
    }

    protected void postExecExertion(Exertion ex, Exertion result)
            throws ExertionException, SignatureException {
        ((ServiceExertion) result).stopExecTime();
        try {
            ((NetJob) xrt).setMogramAt(result, ex.getIndex());
            ServiceExertion ser = (ServiceExertion) result;
            if (ser.getStatus() > FAILED && ser.getStatus() != SUSPENDED) {
                ser.setStatus(DONE);
                collectOutputs(result);
            }
            result.getControlContext().appendTrace((provider.getProviderName() != null
                    ? provider.getProviderName() + " " : "")
                    + "done: " + ex.getName() + " dispatcher: " + getClass().getName());

        } catch (Exception e) {
            throw new ExertionException(e);
        }
        changeDoneExertionIndex(result.getIndex());
    }

    protected void handleError(Exertion exertion) throws RemoteException {
        if (exertion != xrt)
            ((NetJob) xrt).setMogramAt(exertion,
                    exertion.getIndex());

        // notify monitor about failure
        MonitoringSession monSession = MonitorUtil.getMonitoringSession(exertion);
        if (exertion.isMonitorable() && monSession!=null) {
            logger.debug("Notifying monitor about failure of exertion: " + exertion.getName());
            try {
                monSession.changed(exertion.getContext(), exertion.getControlContext(), exertion.getStatus());
            } catch (Exception ce) {
                logger.warn("Unable to notify monitor about failure of exertion: " + exertion.getName() + " " + ce.getMessage());
            }
        }
    }

    private void cleanRemainingFailedExertions(Uuid id) {
        logger.debug("clean remaining failed exertions for {}", id);
        ExertionEnvelop template = ExertionEnvelop.getParentTemplate(id, null);
        ExertionEnvelop ee = null;

        do {
            try {
                logger.debug("take envelop {}", template);
                ee = takeEnvelop(template);
            } catch (ExertionException e) {
                logger.warn("Error while taking {}", template);
            }
        } while (ee != null);
    }

    protected void executeMasterExertion() throws
            ExertionException, SignatureException {
        if (masterXrt == null)
            return;
        logger.info("executeMasterExertion ==============> SPACE EXECUTE MASTER EXERTION");
        try {
            writeEnvelop(masterXrt);
        } catch (RemoteException re) {
            re.printStackTrace();
            logger.warn("Space died....resetting space");
            space = SpaceAccessor.getSpace();
            if (space == null) {
                throw new ExertionException("NO exertion space available!");
            }
            try {
                writeEnvelop(masterXrt);
            } catch (Exception e) {
                logger.warn("Space died....could not recover", e);
            }
        }

        ExertionEnvelop template = ExertionEnvelop.getTakeTemplate(
                masterXrt.getParentId(), masterXrt.getId());

        ExertionEnvelop result = takeEnvelop(template);
        logger.debug("executeMasterExertion MASTER EXERTION RESULT RECEIVED");
        if (result != null && result.exertion != null) {
            postExecExertion(masterXrt, result.exertion);
        }
    }
}
