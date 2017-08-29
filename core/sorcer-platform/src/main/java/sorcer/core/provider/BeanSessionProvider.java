package sorcer.core.provider;

import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.*;

import javax.security.auth.Subject;
import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 8/29/17.
 */
public class BeanSessionProvider extends ServiceProvider {

    private ServiceSignature beanSignature;

    public BeanSessionProvider() throws RemoteException {
        super();
    }

    /**
     * Required constructor for Jini 2 NonActivatableServiceDescriptors
     *
     * @param args
     * @param lifeCycle
     * @throws Exception
     */
    public BeanSessionProvider(String[] args, LifeCycle lifeCycle) throws Exception {
        super(args, lifeCycle);
    }

    public Signature getBeanSignature() {
        return beanSignature;
    }

    public void setBeanSignature(Signature beanSignature) {
        this.beanSignature = (ServiceSignature)beanSignature;
    }


    /** {@inheritDoc} */
    public ServiceExertion execute(Exertion task) throws TransactionException,
            ExertionException {
        return execute(task, null);
    }

    /** {@inheritDoc}
     * @throws ConfigurationException
     * @throws RemoteException */
    public ServiceExertion execute(Exertion task, Transaction transaction)
            throws ExertionException  {
        try {
            return (Task) new ControlFlowManager((Exertion) task, delegate)
                    .process();
        } catch (Exception e) {
            throw new ExertionException(e);
        }
    }

    @Override
    public Mogram exert(Mogram mogram, Transaction txn, Arg... args) throws TransactionException,
            ExertionException, RemoteException {
        if (mogram instanceof Task) {
            ServiceContext cxt = null;
            Object bean = null;
            try {
                cxt = (ServiceContext) mogram.getDataContext();
                cxt.updateContextWith(mogram.getProcessSignature().getInConnector());
                Uuid id = cxt.getId();
                ProviderSession ps = sessions.get(id);
                if (ps == null) {
                    ps = new ProviderSession(id);
                    sessions.put(id, ps);
                    try {
                        bean = sorcer.co.operator.instance(beanSignature);
                    } catch (Exception e) {
                        throw new ExertionException(e);
                    }
                    ps.setAttribute(((Identifiable)bean).getId().toString(), bean);
                }
            } catch (ContextException e) {
                e.printStackTrace();
            }
        } else if (mogram instanceof Context) {
            return serviceContextOnly((Context)mogram);
        }

        // TODO transaction handling to be implemented when needed
        // TO DO HANDLING SUSSPENDED mograms
        // if (((ServiceExertion) exertion).monitorSession != null) {
        // new Thread(new ServiceThread(exertion, this)).start();
        // return exertion;
        // }
        Exertion exertion = (Exertion)mogram;
        // when service Locker is used
        if (delegate.mutualExlusion()) {
            Object mutexId = ((ControlContext)exertion.getControlContext()).getMutexId();
            if (mutexId == null) {
                exertion.getControlContext().appendTrace(
                        "mutex required by: " + getProviderName() + ":"
                                + getProviderID());
                return exertion;
            } else if (!(mutexId.equals(delegate.getServiceID()))) {
                exertion.getControlContext().appendTrace(
                        "invalid mutex for: " + getProviderName() + ":"
                                + getProviderID());
                return exertion;
            }
        }
        // allow provider to leave a trace
        // exertion.getControlContext().appendTrace(
        // delegate.mutualExlusion() ? "mutex in: "
        // + getProviderName() + ":" + getProviderID()
        // : "in: " + getProviderName() + ":"
        // + getProviderID());
        Exertion out = exertion;
        try {
            out = doExertion(exertion, txn);
        } catch (Exception e) {
            logger.error("{} failed", getProviderName(), e);
            out.reportException(new ExertionException(getProviderName() + " failed", e));
        }
        return out;
    }

    /** {@inheritDoc} */
    public boolean isAuthorized(Subject subject, Signature signature) {
        return true;
    }
}
