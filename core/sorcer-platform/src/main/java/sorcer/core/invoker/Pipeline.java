package sorcer.core.invoker;

import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Pipeline extends ServiceInvoker<Context> {
    private List<Opservice> opservices = new ArrayList<>();

    public Pipeline(Opservice... opservices) {
        this(null, opservices);
    }

    public Pipeline(String name, Opservice... opservices) {
        super(name);
        for (Opservice eval : opservices) {
            this.opservices.add(eval);
        }
    }

    public Pipeline(List<Opservice> opservices) {
        this(null, opservices);
    }

    public Pipeline(String name, List<Opservice> Opseropservicesvice) {
        super(name);
        this.opservices = opservices;
    }

    /* (non-Javadoc)
	 * @see sorcer.service.Evaluation#execute(sorcer.service.Args[])
	 */
    @Override
    public Context evaluate(Arg... args) throws InvocationException,
            RemoteException {

        Context out = Arg.selectContext(args);
        if (out == null) {
            out = new PositionalContext(getClass().getSimpleName() + "-" + name);
        }

        Context returnContext = null;
        if (contextReturn != null && contextReturn.getDataContext() != null) {
            returnContext = contextReturn.getDataContext();
            try {
                out.append(returnContext);
            } catch (ContextException e) {
                throw new InvocationException(e);
            }
        }

        Context cxt = null;
        Object opout =  null;
        for (Opservice opsrv : opservices) {
            try {
                if (invokeContext != null && invokeContext.size() > 0) {
                    if (opsrv instanceof Scopable) {
                        if (((Scopable) opsrv).getScope() == null) {
                            ((Scopable) opsrv).setScope(invokeContext);
                        } else {
                            ((Scopable) opsrv).getScope().append(invokeContext);

                        }
                    }
                }
                if (returnContext != null) {
                    if (((Scopable) opsrv).getScope() == null) {
                        ((Scopable) opsrv).setScope(returnContext);
                    } else {
                        ((Scopable) opsrv).getScope().append(returnContext);
                    }
                }

                if (opsrv instanceof Invocation) {
                    opout = ((Invocation) opsrv).invoke(out, args);
                } else if (opsrv instanceof Evaluation) {
                    opout = ((Evaluation) opsrv).evaluate(args);
                } else {
                    if (opsrv instanceof Signature) {
                        opout = opsrv.execute(out);
                    } else {
                        opout = opsrv.execute(args);
                    }
                }

                if (opout instanceof Context) {
                    out.append((Context)opout);
                } else {
                    ((ServiceContext) out).put(((Identifiable) opsrv).getName(), opout);
                }
            } catch (ServiceException e) {
                throw new InvocationException(e);
            }
        }
        return out;
    }

    public List<Opservice> getEvaluators() {
        return opservices;
    }

    public void setEvaluators(List<Opservice> opservices) {
        this.opservices = opservices;
    }


}
