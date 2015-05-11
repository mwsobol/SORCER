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

import net.jini.config.ConfigurationException;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.*;
import sorcer.core.provider.rendezvous.RendezvousBean;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.jobber.JobberAccessor;
import sorcer.service.spacer.SpacerAccessor;
import sorcer.util.AccessorException;

import java.rmi.RemoteException;
import java.util.List;

import static sorcer.eo.operator.task;

/**
 * The ControlFlowManager class is responsible for handling control flow
 * exertions ({@link Conditional}, {@link NetJob}, {@link NetTask}).
 * 
 * This class is used by the {@link sorcer.core.provider.exerter.ServiceShell} class for executing
 * {@link Exertions}.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ControlFlowManager {

    /**
     * Logger for this ExerterController logging.
     */
    protected static final Logger logger = LoggerFactory
            .getLogger(ControlFlowManager.class.getName());

	/**
	 * ExertionDelegate reference needed for handling exertions.
	 */
	protected ProviderDelegate delegate;

	/**
	 * The Exertion that is going to be executed.
	 */
	protected Exertion exertion;

    /**
     * Reference to a jobber proxy if available.
     */
    protected Jobber jobber;

	/**
	 * Reference to a concatenator proxy if available.
	 */
	protected Concatenator concatenator;

	/**
	 * Reference to a spacer service.
	 */
	protected Spacer spacer;

	/**
	 * Default Constructor.
	 */
	public ControlFlowManager() {
	}

	/**
	 * Overloaded constructor which takes in an Exertion and an ExerterDelegate.
	 * 
	 * @param exertion
	 *            Exertion
	 * @param delegate
	 *            ExerterDelegate
     * @throws ConfigurationException
     * @throws RemoteException
     */
	public ControlFlowManager(Exertion exertion, ProviderDelegate delegate)
            throws RemoteException, ConfigurationException {
		this.delegate = delegate;
		this.exertion = exertion;
        init();
	}

    /**
     * Overloaded constructor which takes in an Exertion, ExerterDelegate, and
     * Spacer. This constructor is used when handling {@link sorcer.service.Job}s.
     *
     * @param exertion
     *            Exertion
     * @param delegate
     *            ExerterDelegate
     * @param rendezvousBean
     *            Rendezvous
     * @throws ConfigurationException
     * @throws RemoteException
     */
    public ControlFlowManager(Exertion exertion, ProviderDelegate delegate,
                              RendezvousBean rendezvousBean) throws RemoteException, ConfigurationException {
        this.delegate = delegate;
        this.exertion = exertion;
        if (rendezvousBean instanceof Concatenator) {
            concatenator = (Concatenator)rendezvousBean;
        }
        else if (rendezvousBean instanceof Spacer){
            spacer = (Spacer) rendezvousBean;
        }
        else if (rendezvousBean instanceof Jobber) {
            jobber = (Jobber) rendezvousBean;
        }
        init();
    }


    /**
     * Overloaded constructor which takes in an Exertion, ExerterDelegate, and
     * Spacer. This constructor is used when handling {@link sorcer.service.Job}s.
     *
     * @param exertion
     *            Exertion
     * @param delegate
     *            ExerterDelegate
 	 * @param jobber
	 *            Jobber
     * @throws ConfigurationException
     * @throws RemoteException
     */
	public ControlFlowManager(Exertion exertion, ProviderDelegate delegate,
			Jobber jobber) throws RemoteException, ConfigurationException {
        this(exertion, delegate);
		this.jobber = jobber;
	}
	/**
	 * Overloaded constructor which takes in an Exertion, ExerterDelegate, and
	 * Concatenator. This constructor is used when handling {@link sorcer.service.Block}s.
	 * 
	 * @param exertion
	 *            Exertion
	 * @param delegate
	 *            ExerterDelegate
	 * @param concatenator
	 *            Concatenator
     * @throws ConfigurationException
     * @throws RemoteException
	 */
	public ControlFlowManager(Exertion exertion, ProviderDelegate delegate,
			Concatenator concatenator) throws RemoteException, ConfigurationException {
		this(exertion, delegate);
		this.concatenator = concatenator;
	}

    private void init() throws RemoteException, net.jini.config.ConfigurationException {
        if (concatenator==null) {
            Concatenator c = (Concatenator) delegate.getBean(Concatenator.class);
            if (c != null) {
                concatenator = c;
            }
        }
        if (jobber==null) {
            Jobber j = (Jobber) delegate.getBean(Jobber.class);
            if (j != null) {
                jobber = j;
            }
        }
        if (spacer==null) {
            Spacer s = (Spacer) delegate.getBean(Spacer.class);
            if (s != null) {
                spacer = s;
            }
        }
    }


    /**
     * Process the Exertion accordingly if it is a job, task, or a Conditional
     * Exertion.
     *
     * @return Exertion the result
     * @see NetJob
     * @see NetTask
     * @see Conditional
     * @throws ExertionException
     *             exception from other methods
     */
    public Mogram process() throws ExertionException {
        logger.info("process exertion: " + exertion.getName());
        try {
            Mogram result = null;
            if (exertion.isConditional()) {
                logger.info("exertion Conditional");
                result = doConditional(exertion);
                logger.info("exertion Conditional; result: " + result);
            } else if (exertion.isJob()) {
                logger.info("exertion isJob()");
                result = doRendezvousExertion((Job) exertion);
                logger.info("exertion isJob(); result: " + result);
            } else if (exertion.isBlock()) {
                logger.info("exertion isBlock()");
                result = doBlock((Block) exertion);
                logger.info("exertion isBlock(); result: " + result);
            } else if (exertion.isTask()) {
                logger.info("exertion isTask()");
                result = doTask((Task) exertion);
                logger.info("exertion isTask(); result: " + result);
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (ExertionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExertionException(e.getMessage(), e);
        }
    }

    /**
     * This method delegates the doTask method to the ExertionDelegate.
     *
     * @param task
     *            ServiceTask
     * @return ServiceTask
     * @throws RemoteException
     *             exception from ExertionDelegate
     * @throws ExertionException
     *             exception from ExertionDelegate
     * @throws SignatureException
     *             exception from ExertionDelegate
     * @throws TransactionException
     * @throws ContextException
     */
    public Task doTask(Task task) throws RemoteException, MogramException,
            SignatureException, TransactionException, ContextException {
        Task result;
        if (task.getControlContext().getAccessType() == Access.PULL) {
            result = (Task)doRendezvousExertion(task);
        } else if (delegate != null) {
            result = delegate.doTask(task, null);
        }
        else if (task.isConditional())
            result = doConditional(task);
        else
            result = doBatchTask(task);

        return result;
    }

	public Block doBlock(Block block) throws RemoteException, MogramException,
			SignatureException, TransactionException, ContextException {


        if (concatenator == null) {
            String spacerName = block.getRendezvousName();
            if (spacerName != null) {
                concatenator = Accessor.getService(spacerName, Concatenator.class);
            } else {
                try {
                    concatenator = Accessor.getService(Concatenator.class);
                } catch (Exception x) {
                    throw new ExertionException("Could not find Concatenator", x);
                }
            }
            logger.info("Got Concatenator: " + concatenator);
            return (Block)concatenator.service(block, null);
        }
        return (Block)((ServiceConcatenator)concatenator).execute(block);
	}

    /**
     * Selects a Jobber or Spacer for exertion processing. If own Jobber or
     * Spacer is not available then fetches one and forwards the exertion for
     * processing.
     *
     * @param xrt
     * 			the exertion to be processed
     * @throws RemoteException
     * @throws ExertionException
     */
    public Mogram doRendezvousExertion(ServiceExertion xrt) throws RemoteException, MogramException {
        try {
            if (xrt.isSpacable()) {
                logger.info("exertion isSpacable");

                if (spacer == null) {
                    String spacerName = xrt.getRendezvousName();
                    Spacer spacerService;
                    if (spacerName != null) {
                        spacerService = SpacerAccessor.getSpacer(spacerName);
                    } else {
                        try {
                            spacerService = SpacerAccessor.getSpacer();
                        } catch (Exception x) {
                            throw new ExertionException("Could not find Spacer", x);
                        }
                    }
                    logger.info("Got Spacer: " + spacerService);
                    return spacerService.service(xrt, null);
                }
				Mogram job = ((Executor)spacer).execute(xrt, null);
                logger.info("spacable exerted = " + job);
                return job;
            }
            else {
                logger.info("exertion NOT Spacable");
                if (jobber == null) {
                    String jobberName = xrt.getRendezvousName();
					Jobber jobberService;
                    if (jobberName != null)
                        jobberService = JobberAccessor.getJobber(jobberName);
                    else
                        try {
                            jobberService = JobberAccessor.getJobber();
                        } catch (AccessorException e) {
                            throw new ExertionException("Could not find Jobber", e);
                        }
                    logger.info("Got Jobber: " + jobber);
                    return jobberService.service(xrt, null);
                }
				Mogram job = ((Executor)jobber).execute(xrt, null);
                logger.info("job exerted = " + job);
                return job;
            }
        } catch (TransactionException e) {
            logger.error( "Error", e);
        }
        return null;
    }
    /**
     * This method handles the {@link Conditional} Exertions. It determines if
     * the conditional Exertion: {@link OptExertion}, {@link AltExertion}, and
     * {@link LoopExertion}.
     *
     * @param exertion
     *            Conditional type Exertion
     * @return Exertion
     * @throws SignatureException
     * @throws ExertionException
     * @throws RemoteException
     */
    public Task doConditional(Exertion exertion) throws RemoteException,
            MogramException, SignatureException {
        return ((Task) exertion).doTask();
    }

    /*
     * This mehtod saves all the data nodes of a context and put it on a Map.
     *
     * @param mapBackUp
     *            HashMap where the ServiceContext data nodes are saved
     * @param context
     *            ServiceContext to be saved into the HashMap

    public static void saveState(Map<String, Object> mapBackUp, Context context) {
        try {
            Enumeration e = context.contextPaths();
			String path;
            while (e.hasMoreElements()) {
				path = (String) e.nextElement();
				mapBackUp.put(path, context.get(path));
            }
        } catch (ContextException ce) {
            logger.info("problem saving state of the ServiceContext " + ce);
            ce.printStackTrace();
        }
    }
*/
    /*
	 * Copies the backup map of the dataContext to the passed dataContext.
     *
     * @param mapBackUp
     *            Saved HashMap which is used to restore from
     * @param context
     *            ServiceContext that gets restored from the saved HashMap

    public static void restoreState(Map<String, Object> mapBackUp,
                                    Context context) {
        Iterator iter = mapBackUp.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String path = (String) entry.getKey();
			Object value = entry.getValue();

            try {
                context.putValue(path, value);
            } catch (ContextException e) {
                e.printStackTrace();
            }
        }
    }
*/

    /*
	 * Copies the data nodes from one dataContext to another (shallow copy).
     *
     * @param fromContext
     *            ServiceContext
     * @param toContext
     *            ServiceContext

    public static void copyContext(Context fromContext, Context toContext) {
        try {
            Enumeration e = fromContext.contextPaths();
			String path;

            while (e.hasMoreElements()) {
				path = (String) e.nextElement();
                toContext.putValue(path, fromContext.getValue(path));
            }
        } catch (ContextException ce) {
            ce.printStackTrace();
        }
    }
*/

    /*
     * Checks if the Exertion is valid for this provider. Returns true if it is
     * valid otherwise returns false.
     *
     * @param exertion
     *            Exertion interface
     * @return boolean

    public boolean isValidExertion(Exertion exertion) {
        String pn = exertion.getProcessSignature().getProviderName();

        if (!(pn == null || pn.equals(SorcerConstants.NULL) || SorcerConstants.ANY
                .equals(pn.trim()))) {
            if (!pn.equals(delegate.config.getProviderName()))
                return false;
        }

        for (int i = 0; i < delegate.publishedServiceTypes.length; i++) {
            if (delegate.publishedServiceTypes[i].equals(exertion
                    .getProcessSignature().getServiceType()))
                return true;
        }

        return false;
    }

    public void setConcatenator(ServiceConcatenator concatenator) {
        this.concatenator = concatenator;
    }

    public void setJobber(ServiceJobber rendezvous) {
        this.rendezvous = rendezvous;
    }

    public void setSpacer(Spacer spacer) {
        this.spacer = spacer;
    }
*/

    /*
     * Traverses the Job hierarchy and reset the task status to INITIAL.
     *
     * @param exertion
     *            Either a task or job
    public void resetExertionStatus(Exertion exertion) {
		if (exertion.isTask()) {
            ((Task) exertion).setStatus(Exec.INITIAL);
		} else if (exertion.isJob()) {
            for (int i = 0; i < ((Job) exertion).size(); i++) {
                this.resetExertionStatus(((Job) exertion).get(i));
            }
        }
    }
*/

    public Task doBatchTask(Task task) throws MogramException,
            SignatureException, RemoteException, ContextException {
        Fidelity tf = task.getFidelity();
        task.correctBatchSignatures();
        task.startExecTime();
        // append context from Contexters
        if (task.getApdProcessSignatures().size() > 0) {
            Context cxt = apdProcess(task);
            cxt.setExertion(task);
            task.setContext(cxt);
        }
        // do preprocessing
        if (task.getPreprocessSignatures().size() > 0) {
            Context cxt = preprocess(task);
            cxt.setExertion(task);
            task.setContext(cxt);
        }
        // execute service task
		Fidelity<Signature> ts = new Fidelity<Signature>();
        Signature tsig = task.getProcessSignature();
        ((ServiceContext)task.getContext()).setCurrentSelector(tsig.getSelector());
        ((ServiceContext)task.getContext()).setCurrentPrefix(tsig.getPrefix());

        ts.getSelects().add(tsig);
        task.setFidelity(ts);
        if (tsig.getReturnPath() != null)
            ((ServiceContext)task.getContext()).setReturnPath(tsig.getReturnPath());

        task = task.doTask();
        if (task.getStatus() <= Exec.FAILED) {
            task.stopExecTime();
            ExertionException ex = new ExertionException("Batch service task failed: "
                    + task.getName());
            task.reportException(ex);
            task.setStatus(Exec.FAILED);
            task.setFidelity(tf);
            return task;
        }
        task.setFidelity(tf);
        // do postprocessing
        if (task.getPostprocessSignatures().size() > 0) {
            Context cxt = postprocess(task);
            cxt.setExertion(task);
            task.setContext(cxt);
        }
        if (task.getStatus() <= Exec.FAILED) {
            task.stopExecTime();
            ExertionException ex = new ExertionException("Batch service task failed: "
                    + task.getName());
            task.reportException(ex);
            task.setStatus(Exec.FAILED);
            task.setFidelity(tf);
            return task;
        }
        task.setFidelity(tf);
        task.stopExecTime();
        return task;
    }

    private Context apdProcess(Task task) throws ExertionException, ContextException {
        return processContinousely(task, task.getApdProcessSignatures());
    }

    private Context preprocess(Task task) throws ExertionException, ContextException {
        return processContinousely(task, task.getPreprocessSignatures());
    }

    private Context postprocess(Task task) throws ExertionException, ContextException {
        return processContinousely(task, task.getPostprocessSignatures());
    }

    public Context processContinousely(Task task, List<Signature> signatures)
            throws ExertionException, ContextException {
        Signature.Type type = signatures.get(0).getType();
        Task t;
        Context shared = task.getContext();
        for (int i = 0; i < signatures.size(); i++) {
            try {
                t = task(task.getName() + "-" + i, signatures.get(i), shared);
                signatures.get(i).setType(Signature.SRV);
                ((ServiceContext)shared).setCurrentSelector(signatures.get(i).getSelector());
                ((ServiceContext)shared).setCurrentPrefix(signatures.get(i).getPrefix());

                Fidelity<Signature> tmp = new Fidelity<Signature>();
                tmp.getSelects().add(signatures.get(i));
                t.setFidelity(tmp);
                t.setContinous(true);
                t.setContext(shared);

                logger.info("Sending one of the batch tasks to exert: " + t.getName() + " " + t.getFidelity());
                t = t.doTask();
                signatures.get(i).setType(type);
                shared = t.getContext();
                if (t.getStatus() <= Exec.FAILED) {
                    task.setStatus(Exec.FAILED);
                    ExertionException ne = new ExertionException(
                            "Batch signature failed: " + signatures.get(i));
                    task.reportException(ne);
                    task.setContext(shared);
                    return shared;
                }
            } catch (Exception e) {
                logger.error("Error", e);
                task.setStatus(Exec.FAILED);
                task.reportException(e);
                task.setContext(shared);
                return shared;
            }
        }
        // return the service context of the last exertion
        return shared;
    }
}
