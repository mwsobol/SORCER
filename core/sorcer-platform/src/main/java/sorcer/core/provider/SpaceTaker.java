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
package sorcer.core.provider;

import net.jini.config.Configuration;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.transaction.Transaction;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalManager;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sorcer.core.SorcerConstants;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.loki.exertion.KPEntry;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.monitor.MonitorUtil;
import sorcer.core.monitor.MonitoringSession;
import sorcer.river.TX;
import sorcer.service.*;
import sorcer.service.space.SpaceAccessor;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a class creates a JavaSpace taker that extends the {@link Thread}
 * class and implements the interfaces {@link LeaseListener} and
 * {@link SorcerConstants}
 * 
 * @see Thread
 * @see LeaseListener
 * @see SorcerConstants
 */
public class SpaceTaker implements Runnable {
	static Logger logger = LoggerFactory.getLogger(SpaceTaker.class);

	protected boolean isTransactional;

	protected static long TRANSACTION_LEASE_TIME = TimeUnit.MINUTES.toMillis(1); // 1 minute

	protected long transactionLeaseTimeout = TRANSACTION_LEASE_TIME;

	public final static long SPACE_TIMEOUT = TimeUnit.SECONDS.toMillis(8); // 8 seconds

	protected long spaceTimeout = SPACE_TIMEOUT;

	protected JavaSpace05 space;

	protected SpaceTakerData data;

	protected ExecutorService pool;

    private LeaseRenewalManager lrm;

	// controls the loop of this space worker
	protected volatile boolean keepGoing = true;
    protected boolean remoteLogging;

	public static void doLog(String msg, String threadId, Transaction.Created txn) {
		String newMsg = "\nspace taker log; thread id = " + threadId + "\n"
				+ msg;

		if (txn != null) {
			long expTime = txn.lease.getExpiration();
			long expDuration = expTime - System.currentTimeMillis();
			newMsg = newMsg + "\n\ttxn = " + txn;
			newMsg = newMsg + "\n\tlease = " + txn.lease;
			newMsg = newMsg + "\n\texpires in [s] = " + expDuration / 1000;
		}
		logger.info(newMsg);

	}

	public static class SpaceTakerData {
		public ExertionEnvelop entry;
		public LokiMemberUtil myMemberUtil;
		public Provider provider;
		public String spaceName;
		public String spaceGroup;
		public boolean workerTransactional;
		public boolean noQueue;
		public Object osName = System.getProperty("os.name");
		public List<String> appNames;


		public SpaceTakerData() {
		}

		public SpaceTakerData(ExertionEnvelop entry, LokiMemberUtil member,
							  Provider provider, String spaceName, String spaceGroup,
							  boolean workerIsTransactional, boolean noQueue, String osName, List<String> appNames) {
			this.provider = provider;
			this.entry = entry;
			this.myMemberUtil = member;
			this.spaceName = spaceName;
			this.spaceGroup = spaceGroup;
			this.workerTransactional = workerIsTransactional;
			this.noQueue = noQueue;
			this.osName = osName;
			this.appNames = appNames;
		}

		public String toString() {
			try {
				return entry.describe();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Default constructor. Set the worker thread as a Daemon thread
	 */
	public SpaceTaker() {
        lrm = new LeaseRenewalManager();
	}

	/**
	 * This is a Constructor. It executes the default constructor plus setValue the
	 * provider worker data and executor service pool. The transaction lease
	 * time is setValue and space time outGovernance time is established.
	 * 
	 * @param data
	 *            SpaceDispatcher data
	 * @param pool
	 *            Executor service provides methods to manage termination and
     * @param remoteLogging
     *              Enable logging to a RemoteLoggerManager
	 */
	public SpaceTaker(SpaceTakerData data, ExecutorService pool, boolean remoteLogging) {
		this();
		this.data = data;
		this.pool = pool;
        this.remoteLogging = remoteLogging;
		this.transactionLeaseTimeout = getTransactionLeaseTime();
		this.spaceTimeout = getTimeOut();
		this.isTransactional = data.workerTransactional;
	}

	protected long getTransactionLeaseTime() {
		long lt = TRANSACTION_LEASE_TIME;
		Configuration config = null;
		try {
			config = ((ServiceProvider)data.provider).getProviderConfiguration();
			lt = (Long) config.getEntry(ServiceProvider.COMPONENT,
					ProviderDelegate.WORKER_TRANSACTION_LEASE_TIME, long.class);
		} catch (Exception e) {
			lt = TRANSACTION_LEASE_TIME;
		}
		return lt;
	}

	protected long getTimeOut() {
		long st = SPACE_TIMEOUT;
		Configuration config = null;
		try {
			config = ((ServiceProvider)data.provider).getProviderConfiguration();
			st = (Long) config.getEntry(ServiceProvider.COMPONENT,
					ProviderDelegate.SPACE_TIMEOUT, long.class);
		} catch (Exception e) {
			st = SPACE_TIMEOUT;
		}
		return st;
	}

	
	// fields for taker thread metrics
	//
	private int numThreadsTaker = 0;
	private ArrayList<String> threadIdsTaker = new ArrayList<String>();
	private int numCallsTaker = 0;

	protected synchronized String doThreadMonitorTaker(String threadIdString) {
		
		String prefix;
		if (threadIdString == null) {
			numCallsTaker++;
			numThreadsTaker++;
			prefix = "adding taker thread";
			threadIdString = Integer.toString(numCallsTaker);
			//threadIdString = this.toString();
			threadIdsTaker.add(threadIdString);
		} else {
			numThreadsTaker--;
			prefix = "subtracting taker thread";
			threadIdsTaker.remove(threadIdString);
		}
		
		logger.info("\n\n***SPACE TAKER THREAD: " + prefix + ": total calls = " + numCallsTaker
				+ "\n***" + prefix + ": number of threads running = "
				+ numThreadsTaker + "\n***" + prefix + ": thread ids running = "
				+ threadIdsTaker 
				+ "\nthis = " + this);
		
		return threadIdString;
	}

	// fields for worker thread metrics
	//
	private int numThreadsWorker = 0;
	private ArrayList<String> threadIdsWorker = new ArrayList<String>();
	private int numCallsWorker = 0;	
	
	protected synchronized String doThreadMonitorWorker(String threadIdString) {
		String prefix;
		if (threadIdString == null) {
			numCallsWorker++;
			numThreadsWorker++;
			prefix = "adding worker thread";
			threadIdString = Integer.toString(numCallsWorker);
			threadIdsWorker.add(threadIdString);
		} else {
			numThreadsWorker--;
			prefix = "subtracting worker thread";
			threadIdsWorker.remove(threadIdString);
		}
		logger.info("\n\n***SPACE WORKER THREAD: " + prefix + ": total calls = " + numCallsWorker
				+ "\n***" + prefix + ": number of threads running = "
				+ numThreadsWorker + "\n***" + prefix + ": thread ids running = "
				+ threadIdsWorker
				+ "\nthis = " + this);

		return threadIdString;
	}
	
	public void run() {
		String threadId = doThreadMonitorTaker(null);

		Transaction.Created txnCreated = null;

		while (keepGoing) {
			ExertionEnvelop ee;
            Object envelopNoCast;
			try {
				space = SpaceAccessor.getSpace(data.spaceName);
				if (space == null) {
					Thread.sleep(spaceTimeout / 6);
					continue;
				}

				if (data.noQueue) {
					if (((ThreadPoolExecutor) pool).getActiveCount() != ((ThreadPoolExecutor) pool).getCorePoolSize()) {
                        Transaction tx = null;
						if (isTransactional) {
							txnCreated = TX.createTransaction(transactionLeaseTimeout);
							if (txnCreated == null) {
                                logger.warn("SpaceTaker did not get TRANSACTION thread: {}", threadId);
								Thread.sleep(spaceTimeout / 6);
								continue;
							}
                            tx = txnCreated.transaction;
						}
                        envelopNoCast = space.take(data.entry, tx, spaceTimeout);
                        ee = (ExertionEnvelop) envelopNoCast;
					} else {
                        /* Sleep for whats basically a clock tick to avoid thrashing */
                        Thread.sleep(50);
						continue;
					}
				} else {
					if (isTransactional) {
						txnCreated = TX.createTransaction(transactionLeaseTimeout);
						if (txnCreated == null) {
							doLog("\t***warning: space taker did not get TRANSACTION.",
									threadId, null);
							Thread.sleep(spaceTimeout / 6);
							continue;
						}
						ee = (ExertionEnvelop) space.take(data.entry,
								txnCreated.transaction, spaceTimeout);
					} else {
						ee = (ExertionEnvelop) space.take(data.entry, null,
								spaceTimeout);
					}
				}

				// after 'take' timeout abort transaction and sleep for a while
				// before 'taking' the next exertion
				if (ee == null) {
					if (txnCreated != null) {
						TX.abortTransaction(txnCreated);
						try {
                            Thread.sleep(spaceTimeout / 2);
                        } catch (InterruptedException ie) {
                            keepGoing = false;
                            break;
                        }
					}

					txnCreated = null;
					continue;
				}
                pool.execute(new SpaceWorker(ee, txnCreated, data.provider, remoteLogging));
			} catch (Exception ex) {
                logger.warn("Problem with SpaceTaker", ex);
			}
		}
		
		// remove thread monitor
		doThreadMonitorTaker(threadId);
	}

	synchronized public Transaction.Created createTransaction() {
		return TX.createTransaction(transactionLeaseTimeout);
	}

	protected boolean isAbandoned(Exertion exertion) {
		if (space != null) {
			ExertionEnvelop ee = new ExertionEnvelop();
			ee.parentID = exertion.getParentId();
			ee.state = Exec.POISONED;
			try {
				if (space.readIfExists(ee, null, JavaSpace.NO_WAIT) != null) {
					logger.info("...............dropped poisoned entry...............");
					return true;
				}
			} catch (Exception e) {
				logger.debug("isAbandoned", e);
				// continue on
			}
		}
		return false;
	}

	protected void initDataMember(ExertionEnvelop ee, Transaction txn) {
		try {
			KPEntry ckpeRes = (KPEntry) ee.exertion;
			data.myMemberUtil.setGroupSeqId(ckpeRes.GroupSeqId);
			data.myMemberUtil.takewriteKPExertion(ckpeRes.publicKey,
					data.entry.serviceType);
			data.myMemberUtil.readCCK(data.entry.serviceType);
			ee = data.myMemberUtil.takeEnEE(data.entry, txn);
		} catch (Exception e) {
			logger.debug("run", e);
		}
	}

    public void destroy() {
        keepGoing = false;
    }


    class SpaceWorker implements Runnable {
        private ExertionEnvelop ee;
        private Provider provider;
		private Transaction.Created txnCreated;
		private boolean remoteLogging;

		SpaceWorker(ExertionEnvelop envelope,
                    Transaction.Created workerTxnCreated,
                    Provider provider,
                    boolean remoteLogging)
				throws UnknownLeaseException {
            this.provider = provider;
			ee = envelope;
            this.remoteLogging = remoteLogging;
			if (workerTxnCreated != null) {
				txnCreated = workerTxnCreated;
			}
		}

		public void run() {
            if(remoteLogging)
                MDC.put(SorcerConstants.MDC_SORCER_REMOTE_CALL, SorcerConstants.MDC_SORCER_REMOTE_CALL);
            if (ee.exertion!=null && ee.exertion.getId()!=null)
                MDC.put(SorcerConstants.MDC_MOGRAM_ID, ee.exertion.getId().toString());
            try {
                String prvId = null;
                if (data.provider!=null)
                    prvId = data.provider.getProviderID().toString();
                MDC.put(SorcerConstants.MDC_PROVIDER_ID, prvId);
            } catch (RemoteException re) {
                logger.warn("Problem getting provider ID in SpaceTaker");
            }
			String threadId = doThreadMonitorWorker(null);

			Entry result = doEnvelope(ee,
                                      (txnCreated == null) ? null : txnCreated.transaction, threadId, txnCreated);

			if (result != null) {
				try {
					space.write(result, null, Lease.FOREVER);
				} catch (Exception e) {
                    logger.warn("Error while writing the outDispatcher", e);
                    try {
						TX.abortTransaction(txnCreated);
					} catch (Exception e1) {
                        logger.warn("Error while aborting transaction", e1);
						doThreadMonitorWorker(threadId);
						return;
					}
					doThreadMonitorWorker(threadId);
					return;
				}
				if (txnCreated != null) {
					try {
						TX.commitTransaction(txnCreated);
					} catch (Exception e) {
                        logger.warn("Error while committing transaction", e);
						doThreadMonitorWorker(threadId);
						return;
					}
				}
			} else {			
				doLog("\t***error: doEnvelope returned null.", threadId,
						txnCreated);
				if (txnCreated != null) {
					try {
						TX.abortTransaction(txnCreated);
					} catch (Exception e) {
                        logger.warn("Error while aborting transaction", e);
						doThreadMonitorWorker(threadId);
						return;
					}
				}
			}
			doThreadMonitorWorker(threadId);
            MDC.remove(SorcerConstants.MDC_SORCER_REMOTE_CALL);
            MDC.remove(SorcerConstants.MDC_MOGRAM_ID);
            MDC.remove(SorcerConstants.MDC_PROVIDER_ID);
		}

		public Entry doEnvelope(ExertionEnvelop ee, Transaction transaction, String threadId, Transaction.Created txn) {
			ServiceExertion se;
            ServiceExertion out;
            try {
				ee.exertion.getControlContext().appendTrace("spacer: "+data.provider.getProviderName());
				se = (ServiceExertion) ee.exertion;
                MonitoringSession monSession = MonitorUtil.getMonitoringSession(se);

                if (se.isMonitorable() && se.isTask() && monSession!=null) {
                    try {
                        monSession.init((Monitorable)provider.getProxy());
                        lrm.renewUntil(monSession.getLease(), Lease.FOREVER, TimeUnit.SECONDS.toMillis(30), null);
                    } catch (RemoteException | MonitorException e) {
                        logger.warn("Failed setting up monitoring session", e);
                    }
                }

				if (se instanceof Task) {
					// task for the worker's provider
					out = (((ServiceProvider)data.provider).getDelegate()).doTask((Task) se, transaction);
				} else {
					// delegate it to another collaborating service
					out = data.provider.exert(se, transaction);
				}
				if (out != null) {
					out.setStatus(Exec.DONE);
					ee.state = Exec.DONE;
					ee.exertion = out;
				} else {
					se.setStatus(Exec.ERROR);
					ee.state = Exec.ERROR;
					ee.exertion = se;
					se.reportException(new ExertionError("Not able to exert exertion envelope for exertionID: "+ ee.exertionID));
				}
                if (se.isMonitorable() && se.isTask() && monSession!=null) {
                    try {
                        monSession.changed(se.getContext(), se.getControlContext(), se.getStatus());
                    } catch(RemoteException | MonitorException | ContextException e) {
                        logger.error("Failed updating MonitorSession", e);
                    } finally {
                        try {
                            lrm.remove(monSession.getLease());
                        } catch (UnknownLeaseException e) {
                            logger.warn("Failed removing monitoring session lease", e);
                        }
                    }
                }
            } catch (Throwable th) {
				logger.debug("doEnvelope", th);
				if (th instanceof Exception) {
					ee.state = Exec.FAILED;
					ee.exertion.setStatus(Exec.FAILED);
				} else if (th instanceof Error) {
					ee.state = Exec.ERROR;
					ee.exertion.setStatus(Exec.ERROR);
				}
				ee.exertion.reportException(th);
			}
			return ee;
		}
	}

}