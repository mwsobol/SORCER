package sorcer.core.invoker;

import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Pipeline extends ServiceInvoker<Context> {

    private Context newInvokeContext;

    private boolean isNewInput = false;

    private List<Opservice> opservices = new ArrayList<>();

    public Pipeline(Opservice... opservices) {
        this(null, opservices);
    }

    public Pipeline(String name, Context data, Opservice... opservices) {
        super(name);
        if (data != null) {
            invokeContext = data;
        }
        for (Opservice eval : opservices) {
            this.opservices.add(eval);
        }
    }

    public Pipeline(String name, Opservice... opservices) {
        this(name, null, opservices);
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
        } else {
            try {
                out.append(invokeContext);
            } catch (ContextException e) {
                throw new InvocationException(e);

            }
        }

        Context cxt = null;
        Object opout =  null;
        for (Opservice opsrv : opservices) {
            try {
                if (opsrv instanceof Scopable) {
                    if (((Scopable) opsrv).getScope() == null) {
                        ((Scopable) opsrv).setScope(out);
                    } else {
                        ((Scopable) opsrv).getScope().append(out);

                    }
                }

                if (returnContext != null) {
                    if (((Scopable) opsrv).getScope() == null) {
                        ((Scopable) opsrv).setScope(returnContext);
                    } else {
                        ((Scopable) opsrv).getScope().append(returnContext);
                    }
                }

                if (opsrv instanceof Appender) {
                    Context appendContext = (Context) opsrv.execute();
                    if (((Appender) opsrv).isNew()) {
                        if (((Appender) opsrv).getType().equals(Appender.ContextType.INPUT)) {
                            newInvokeContext = appendContext;
                            isNewInput = true;
                        } else {
                            scope = appendContext;
                        }
                    } else {
                        if (((Appender) opsrv).getType().equals(Appender.ContextType.INPUT)) {
                            out.append(appendContext);
                        } else {
                            scope.append(appendContext);
                        }
                    }
                    continue;
                }

                if (opsrv instanceof Invocation) {
                    if (isNewInput) {
                        opout = ((Invocation) opsrv).invoke(newInvokeContext, args);
                    } else {
                        opout = ((Invocation) opsrv).invoke(out, args);
                    }
                } else if (opsrv instanceof Evaluation) {
                    opout = ((Evaluation) opsrv).evaluate(args);
                } else {
                    if (opsrv instanceof Signature) {
                        if (isNewInput) {
                            opout = opsrv.execute(newInvokeContext);
                        } else {
                            opout = opsrv.execute(out);
                        }
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
