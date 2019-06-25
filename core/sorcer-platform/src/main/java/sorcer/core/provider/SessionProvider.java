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
import java.util.*;

/**
 * Created by Mike Sobolewski on 8/29/17.
 */
public class SessionProvider extends ServiceExerter implements SessionManagement {

    public SessionProvider() throws RemoteException {
        super();
    }

    /**
     * Required constructor for Jini 2 NonActivatableServiceDescriptors
     *
     * @param args
     * @param lifeCycle
     * @throws Exception
     */
    public SessionProvider(String[] args, LifeCycle lifeCycle) throws Exception {
        super(args, lifeCycle);
    }

    /** {@inheritDoc} */
    public ServiceRoutine execute(Routine task) throws TransactionException,
            RoutineException {
        return execute(task, null);
    }

    /** {@inheritDoc}
     * @throws ConfigurationException
     * @throws RemoteException */
    public ServiceRoutine execute(Routine task, Transaction transaction)
            throws RoutineException  {
        try {
            return (Task) new ControlFlowManager((Routine) task, delegate)
                    .process();
        } catch (Exception e) {
            throw new RoutineException(e);
        }
    }

    @Override
    public Mogram exert(Mogram mogram, Transaction txn, Arg... args) throws RoutineException, RemoteException {
        if (mogram instanceof Task) {
            ServiceContext cxt = null;
            try {
                cxt = (ServiceContext) mogram.getDataContext();
                cxt.updateContextWith(mogram.getProcessSignature().getInConnector());
                Uuid id = cxt.getId();
                ProviderSession ps = (ProviderSession) sessions.get(id);
                if (ps == null) {
                    ps = new ProviderSession(id);
                    logger.info("created new session: {}", id);
                    sessions.put(id, ps);
                    try {
                        if (bean == null) {
                            if (delegate.getBeanSignature() != null) {
                                bean = sorcer.co.operator.instance(delegate.getBeanSignature());
                            } else {
                                bean = delegate.getSessionBean().getClass().newInstance();
                            }
                        }
                        ps.setAttribute(id.toString(), bean);
                        logger.info("created new bean: {} for: {}", bean, id);
                    } catch (Exception e) {
                        throw new RoutineException(e);
                    }
                } else {
                    bean = ps.getAttribute(id.toString());
                    cxt.putValue(BEAN_SESSION, id);
                    logger.info("using session: {}", id);
                }
                Paths paths;
                Signature.SessionPaths sessionPaths = cxt.getContextReturn().sessionPaths;
                if (sessionPaths != null) {
                    paths = sessionPaths.getPaths(Signature.Append.class);
                    if (paths != null && paths.size() > 0) {
                        write(ps, cxt, paths);
                    }
                    paths = sessionPaths.getPaths(Signature.Read.class);
                    if (paths != null && paths.size() > 0) {
                        if (paths.size() == 1 && paths.get(0).path.equals("*")) {
                            cxt.getDataContext().append(ps);
                            cxt.getDataContext().remove(id);
                        } else {
                            read(ps, cxt, paths);
                        }
                    }
                }

                Task out = delegate.exertBeanTask((Task) mogram, bean, args);

                if (sessionPaths != null) {
                    paths = sessionPaths.getPaths(Signature.Write.class);
                    if (paths != null && paths.size() > 0) {
                        write(ps, out.getDataContext(), paths);
                    }
                    paths = sessionPaths.getPaths(Signature.State.class);
                    if (paths != null && paths.size() > 0) {
                        if (paths.size() == 1 && paths.get(0).path.equals("*")) {
                            out.getDataContext().append(ps);
                            out.getDataContext().remove(id);
                        } else {
                            read(ps, out.getDataContext(), paths);
                        }
                    }
                }
                return out;
            } catch (ContextException e) {
                mogram.reportException(e);
                e.printStackTrace();
                mogram.setStatus(Exec.FAILED);
                return mogram;
            }
        } else if (mogram instanceof Context) {
            return serviceContextOnly((Context) mogram);
        } else {
            mogram.reportException(new RoutineException("Wrong mogram for: " +  this.getClass().getSimpleName()));
            mogram.setStatus(Exec.ERROR);
            return mogram;
        }
    }

    /** {@inheritDoc} */
    public boolean isAuthorized(Subject subject, Signature signature) {
        return true;
    }

    @Override
    public List<Uuid> getSessionIds() throws RemoteException {
        List<Uuid> list = new ArrayList<>();
        for (Uuid id : sessions.keySet()) {
            list.add(id);
        }
        return list;
    }

    @Override
    public Context getSession(Uuid id) throws RemoteException {
        Iterator<Uuid> si = sessions.keySet().iterator();
        Uuid key;
        while(si.hasNext()) {
            key = si.next();
            if (key.equals(id)) {
                Context session = (Context) sessions.get(id);
                // remove bean of this session
                session.remove(key.toString());
                return session;
            }
        }
        return null;
    }

    @Override
    public void removeSession(String id) throws RemoteException {
        sessions.remove(id);
    }

    @Override
    public void clearSessions() throws RemoteException {
        sessions.clear();
    }

    public Context get(Context session, Context taskContext, List<Path> paths) throws ContextException {
        for (Path p : paths) {
            if (session.containsPath(p.path)) {
                try {
                    taskContext.putOutValue(p.path, taskContext.getValue(p.path));
                } catch (RemoteException e) {
                    throw new ContextException(e);
                }
            }
        }
        return taskContext;
    }

    public Context read(Context session, Context taskContext, Paths paths) throws ContextException {
        Iterator it = ((ServiceContext) session).entryIterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (paths.containsPath((String) e.getKey())) {
                try {
                    taskContext.putInValue((String) e.getKey(), session.getValue((String) e.getKey()));
                } catch (RemoteException ex) {
                    throw new ContextException(ex);
                }
            }
        }
        return taskContext;
    }

    public Context write(Context session, Context taskContext, List<Path> paths) throws ContextException {
       for (Path p : paths) {
            if (taskContext.containsPath(p.path)) {
                try {
                    session.putOutValue(p.path, taskContext.getValue(p.path));
                } catch (RemoteException e) {
                    throw new ContextException(e);
                }
            }
        }
        return taskContext;
    }

}
