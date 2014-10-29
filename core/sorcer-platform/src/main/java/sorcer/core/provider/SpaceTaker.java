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

// Imported classes
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import net.jini.config.Configuration;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.transaction.CannotAbortException;
import net.jini.core.transaction.CannotCommitException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalEvent;
import net.jini.lease.LeaseRenewalManager;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import sorcer.core.SorcerConstants;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.loki.exertion.KPEntry;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.service.Exec;
import sorcer.service.Exertion;
import sorcer.service.ExertionError;
import sorcer.service.ServiceExertion;
import sorcer.service.Task;
import sorcer.util.GenericUtil;
import sorcer.util.ProviderAccessor;

/**
 * This is a class creates a JavaSpace taker that extends the {@link Thread}
 * class and implements the interfaces {@link LeaseListener} and
 * {@link SorcerConstants}
 * 
 * @see Thread
 * @see LeaseListener
 * @see SorcerConstants
 */
public class SpaceTaker extends Thread implements LeaseListener,
		SorcerConstants {
	static protected final String LOKI_ONLY = "CreatorsPublicKey";

	static Logger logger = Logger.getLogger(SpaceTaker.class.getName());

	protected boolean isTransactional;

	protected static long TRANSACTION_LEASE_TIME = 1000 * 60 * 1; // 1 minute

	protected long transactionLeaseTimeout = TRANSACTION_LEASE_TIME;

	public final static long SPACE_TIMEOUT = 1000 * 30; // 1/2 minute

	protected long spaceTimeout = SPACE_TIMEOUT;

	protected JavaSpace05 space;

	protected SpaceTakerData data;

	protected ExecutorService pool;

	protected static LeaseRenewalManager leaseManager;

	// controls the loop of this space worker
	protected volatile boolean keepGoing = true;

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

		public SpaceTakerData() {
		};

		public SpaceTakerData(ExertionEnvelop entry, LokiMemberUtil member,
				Provider provider, String spaceName, String spaceGroup,
				boolean workerIsTransactional, boolean noQueue) {
			this.provider = provider;
			this.entry = entry;
			this.myMemberUtil = member;
			this.spaceName = spaceName;
			this.spaceGroup = spaceGroup;
			this.workerTransactional = workerIsTransactional;
			this.noQueue = noQueue;
		}

		public String toString() {
			return entry.describe();
		}
	}

	/**
	 * Default constructor. Set the worker thread as a Daemon thread
	 */
	public SpaceTaker() {
		setDaemon(true);
	}

	/**
	 * This is a Constructor. It executes the default constructor plus set the
	 * provider worker data and executor service pool. The transaction lease
	 * time is set and space time out time is established.
	 * 
	 * @param data
	 *            SpaceDispatcher data
	 * @param pool
	 *            Executor service provides methods to manage termination and
	 *            tracking progress of one or more asynchronous tasks
	 */
	public SpaceTaker(SpaceTakerData data, ExecutorService pool) {
		this();
		this.data = data;
		this.pool = pool;
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
			threadIdString = new Integer(numCallsTaker).toString();
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
	
	protected static void abortTransaction(Transaction.Created txn) throws UnknownLeaseException, UnknownTransactionException, CannotAbortException, RemoteException {
		leaseManager.remove(txn.lease);
		txn.transaction.abort();
	}

	protected static void commitTransaction(Transaction.Created txn) throws UnknownLeaseException, UnknownTransactionException, CannotCommitException, RemoteException  {
		leaseManager.remove(txn.lease);
		txn.transaction.commit();
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
			threadIdString = new Integer(numCallsWorker).toString();
			//threadIdString = this.toString();
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
	
	
	public void stopTakerThread() {
		keepGoing = false;
	}
	
	public void run() {

		//String threadId = doThreadMonitorTaker(null);
		String threadId = "junk2";
//		doLog("\trun()\n\tisTransactional: " + isTransactional
//				+ "\n\ttransactionLeaseTimeout: " + transactionLeaseTimeout
//				+ "\n\tspaceTimeout: " + spaceTimeout + "\n\tdata.noQueue = "
//				+ data.noQueue, threadId, null);

		Transaction.Created txnCreated = null;

		while (keepGoing) {
			logger.info("space taker in run()...keepGoing = " + keepGoing);
			ExertionEnvelop ee = null;
			try {
				space = ProviderAccessor.getSpace(data.spaceName,
						data.spaceGroup);

				if (space == null) {
//					doLog("\t***warning: space taker did not get SPACE.",
//							threadId, null);
					Thread.sleep(spaceTimeout / 6);
					continue;
				}

				if (data.noQueue) {
					if (((ThreadPoolExecutor) pool).getActiveCount() != ((ThreadPoolExecutor) pool)
							.getCorePoolSize()) {
						if (isTransactional) {
							txnCreated = createTransaction(threadId);
							if (txnCreated == null) {
//								doLog("\t***warning: space taker did not get TRANSACTION.",
//										threadId, null);
								Thread.sleep(spaceTimeout / 6);
								continue;
							}
							ee = (ExertionEnvelop) space.take(data.entry,
									txnCreated.transaction, spaceTimeout);
						} else {
							ee = (ExertionEnvelop) space.take(data.entry, null,
									spaceTimeout);
						}
					} else {
						continue;
					}
				} else {
					if (isTransactional) {
						txnCreated = createTransaction(threadId);
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

						//doLog("\taborting txn...", threadId, txnCreated);
						abortTransaction(txnCreated);
						//doLog("\tDONE aborting txn.", threadId, txnCreated);
						
						Thread.sleep(spaceTimeout / 2);
					}
					
					txnCreated = null;
					continue;
				}

				if (isTransactional) {
					//doLog("\tgoing to create space worker w/txn...", threadId, null);
					pool.execute(new SpaceWorker(ee, txnCreated));
				} else {
					//doLog("\tgoing to create space worker NO TXN...", threadId, null);
					pool.execute(new SpaceWorker(ee, null));
				}
			} catch (Exception ex) {
				//logger.info("END LOOP SPACE TAKER EXCEPTION");
				//ex.printStackTrace();
				continue;
			}
		}
		
		// remove thread monitor
		//doThreadMonitorTaker(threadId);
		logger.info("space taker exiting run()...keepGoing = " + keepGoing);
	}

	synchronized public Transaction.Created createTransaction() {
		return createTransaction(null);
	}
	
	synchronized public Transaction.Created createTransaction(String threadId) {
		if (leaseManager == null) {
			leaseManager = new LeaseRenewalManager();
		}
		try {
			TransactionManager tManager = ProviderAccessor
					.getTransactionManager();
			if (tManager == null) {
				return null;
			}
			Transaction.Created created = TransactionFactory.create(tManager,
					transactionLeaseTimeout);

//			doLog("\tcreated transaction", threadId, created);

			leaseManager.renewFor(created.lease, Lease.FOREVER, transactionLeaseTimeout, this);

			return created;
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (LeaseDeniedException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected boolean isAbandoned(Exertion exertion) {
		if (space != null) {
			ExertionEnvelop ee = new ExertionEnvelop();
			ee.parentID = ((ServiceExertion) exertion).getParentId();
			ee.state = Exec.POISONED;
			try {
				if (space.readIfExists(ee, null, JavaSpace.NO_WAIT) != null) {
					logger.info("...............dropped poisoned entry...............");
					return true;
				}
			} catch (Exception e) {
				logger.throwing(this.getClass().getName(), "isAbandoned", e);
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
			logger.throwing(SpaceTaker.class.getName(), "run", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.jini.lease.LeaseListener#notify(net.jini.lease.LeaseRenewalEvent)
	 */
	@Override
	public void notify(LeaseRenewalEvent e) {
		// Do nothing. It happens when a space providers is destroyed
//		logger.severe("########### space transaction lost its lease: "
//				+ e.getLease());
	}

	class SpaceWorker implements Runnable {
		private ExertionEnvelop ee;
		private Transaction.Created txnCreated;
		

		SpaceWorker(ExertionEnvelop envelope,
				Transaction.Created workerTxnCreated)
				throws UnknownLeaseException {
			ee = envelope;
			if (workerTxnCreated != null) {
				txnCreated = workerTxnCreated;
				// leaseManager.setExpiration(txnCreated.lease,
				// System.currentTimeMillis() + transactionLeaseTimeout);
				Exception e = new RuntimeException("");
				
			}
		}
		
		public void run() {
			//String threadId = doThreadMonitorWorker(null);
			String threadId = "junk";
			
			//if (txnCreated != null)
//			doLog("\tcalling doEnvelope()...", threadId, txnCreated);
			Entry result = doEnvelope(ee, (txnCreated == null) ? null
					: txnCreated.transaction, threadId, txnCreated);
//			doLog("\tDONE calling doEnvelope(); result = " + result, threadId, txnCreated);

			if (result != null) {
//				doLog("\tcalling space.write()...", threadId, txnCreated);
				try {
					space.write(result, null, Lease.FOREVER);
				} catch (Exception e) {
//					doLog("\t***error: calling space.write().", threadId,
//							txnCreated);
					e.printStackTrace();
					try {
						if (txnCreated != null)
							abortTransaction(txnCreated);
					} catch (Exception ew) {
						ew.printStackTrace();
					}
					doThreadMonitorWorker(threadId);
					return;
				}
//				doLog("\tDONE calling space.write().", threadId, txnCreated);
				if (txnCreated != null) {
					try {
						commitTransaction(txnCreated);
					} catch (Exception ec) {
						ec.printStackTrace();
					}
				}
			} else {			
				doLog("\t***error: doEnvelope returned null.", threadId,
						txnCreated);
				if (txnCreated != null) {
					try {
						abortTransaction(txnCreated);
					} catch (Exception ea) {
						ea.printStackTrace();
					}
				}
			}
			doThreadMonitorWorker(threadId);
		}

		public void doLog(String msg, String threadId, Transaction.Created txn) {
			String newMsg = "\n\tspace worker log; thread id = " + threadId + "\n"
					+ msg;

			if (txn != null) {
				long expTime = txn.lease.getExpiration();
				long expDuration = expTime - System.currentTimeMillis();
				newMsg = newMsg + "\n\t\ttxn = " + txn;
				newMsg = newMsg + "\n\t\tlease = " + txn.lease;
				newMsg = newMsg + "\n\t\texpires in [s] = " + expDuration / 1000;
			}
			logger.info(newMsg);

		}
		
		public Entry doEnvelope(ExertionEnvelop ee, Transaction transaction,
				String threadId, Transaction.Created txn) {
			ServiceExertion se = null, out = null;
			try {
				// logger.info("\n----SpaceWorker>>execute invoked");
				ee.exertion.getControlContext().appendTrace(
						"taken by: " + data.provider.getProviderName() + ":"
								+ data.provider.getProviderID());
				se = (ServiceExertion) ee.exertion;

				if (se instanceof Task) {
					// task for the worker's provider
					out = ((ProviderDelegate) ((ServiceProvider) data.provider)
							.getDelegate()).doTask((Task) se, transaction);
				} else {
					// delegate it to another collaborating service
					out = (ServiceExertion) data.provider.service(se,
							transaction);
				}
				if (out != null) {
					out.setStatus(Exec.DONE);
					ee.state = Exec.DONE;
					ee.exertion = out;
				} else {
					se.setStatus(Exec.ERROR);
					ee.state = Exec.ERROR;
					ee.exertion = se;
					se.reportException(new ExertionError(
							"Not able to execute exertion envelope for exertionID: "
									+ ee.exertionID));
				}
			} catch (Throwable th) {
				logger.throwing(this.getClass().getName(), "doEnvelope", th);
				th.printStackTrace();
				if (th instanceof Exception) {
					ee.state = Exec.FAILED;
					((ServiceExertion) ee.exertion).setStatus(Exec.FAILED);
				} else if (th instanceof Error) {
					ee.state = Exec.ERROR;
					((ServiceExertion) ee.exertion).setStatus(Exec.ERROR);
				}
				((ServiceExertion) ee.exertion).reportException(th);
			}
			return ee;
		}
	}

}