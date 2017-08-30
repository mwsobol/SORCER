package sorcer.core.provider;

import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import sorcer.core.context.ServiceContext;
import sorcer.service.*;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Mike Sobolewski on 8/29/17.
 */
public class SessionBeanProvider extends ServiceProvider implements SessionManagement {

    public SessionBeanProvider() throws RemoteException {
        super();
    }

    /**
     * Required constructor for Jini 2 NonActivatableServiceDescriptors
     *
     * @param args
     * @param lifeCycle
     * @throws Exception
     */
    public SessionBeanProvider(String[] args, LifeCycle lifeCycle) throws Exception {
        super(args, lifeCycle);
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
                    logger.info("created new session: {}", id);
                    sessions.put(id, ps);
                    try {
                        if (delegate.getBeanSignature() != null) {
                            bean = sorcer.co.operator.instance(delegate.getBeanSignature());
                        } else {
                            bean = delegate.getSessionBean().getClass().newInstance();
                        }
                        logger.info("created new bean: {} for: {}", bean, id);
                    } catch (Exception e) {
                        throw new ExertionException(e);
                    }
                    ps.setAttribute(id.toString(), bean);
                } else {
                    bean = ps.getAttribute(id.toString());
                    logger.info("using session: {}", id);
                }
                if (cxt.containsPath(ServiceSession.SESSION_READ)) {
                    read(ps, cxt, (Context) cxt.get(ServiceSession.SESSION_READ));
                }
                Task out = delegate.exertBeanTask((Task) mogram, bean, args);
                if (cxt.containsPath(ServiceSession.SESSION_WRITE)) {
                    write(ps, out.getDataContext(), (Context) cxt.get(ServiceSession.SESSION_WRITE));
                }
            } catch (ContextException e) {
                mogram.reportException(e);
                e.printStackTrace();
            }
        } else if (mogram instanceof Context) {
            return serviceContextOnly((Context) mogram);
        }
        mogram.setStatus(Exec.FAILED);
        return mogram;
    }

    /** {@inheritDoc} */
    public boolean isAuthorized(Subject subject, Signature signature) {
        return true;
    }

    @Override
    public Set getSessions() throws RemoteException {
        return sessions.keySet();
    }

    @Override
    public Context getSession(String id) throws RemoteException {
        Iterator<Uuid> si = sessions.keySet().iterator();
        Uuid key;
        while(si.hasNext()) {
            key = si.next();
            if (key.toString().equals(id)) {
                Context session = sessions.get(key);
                // remove bean of this session
                session.remove(key.toString());
                return session;
            }
        }
        return null;
    }

    @Override
    public Object get(String id, String key) throws RemoteException {
        Context session = getSession(id);
        return session.get(key);
    }

    @Override
    public void remove(String id) throws RemoteException {
        Iterator<Uuid> si = sessions.keySet().iterator();
        Uuid key;
        while(si.hasNext()) {
            key = si.next();
            if (key.toString().equals(id)) {
                sessions.remove(key);
                break;
            }
        }
    }

    @Override
    public void clear() throws RemoteException {
        sessions.clear();
    }

    public Context read(Context session, Context taskContext, Context connector) throws ContextException {
        if (connector != null) {
            Iterator it = ((ServiceContext) session).entryIterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                if (connector.containsPath((String) e.getKey())) {
                    taskContext.putInValue((String) e.getKey(), session.getValue((String) e.getKey()));
                }
            }
        }
        return taskContext;
    }

    public Context write(Context session, Context taskContext, Context connector) throws ContextException {
        if (connector != null) {
            Iterator it = ((ServiceContext) connector).entryIterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                if (taskContext.containsPath((String) e.getKey())) {
                    session.putOutValue((String) e.getKey(), taskContext.getValue((String) e.getKey()));
                }
            }
        }
        return taskContext;
    }

}
