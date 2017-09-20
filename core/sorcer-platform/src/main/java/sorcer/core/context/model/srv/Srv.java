package sorcer.core.context.model.srv;

import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.MogramEntry;
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.ApplicationDescription;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Function;
import sorcer.core.plexus.MorphFidelity;
import sorcer.service.*;
import sorcer.service.Signature.ReturnPath;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Serviceableness;
import sorcer.service.modeling.func;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Callable;

import static sorcer.eo.operator.task;

/**
 * Created by Mike Sobolewski on 4/14/15.
 */
public class Srv extends Function<Object> implements Functionality<Object>, Serviceableness,
        Comparable<Object>, Reactive<Object>, Serializable, func<Object> {

    private static Logger logger = LoggerFactory.getLogger(Srv.class.getName());

    protected final String name;

    protected Signature srvSignature;

    protected Object srvValue;

    protected String[] paths;

    protected ReturnPath returnPath;

    // srv fidelities
    protected Map<String, Object> fidelities;

    public Srv(String name) {
        super(name);
        this.name = name;
        type = Functionality.Type.SRV;
    }

    public Srv(String name, String path,  Service service, String[] paths) {
        super(path, service);
        this.name = name;
        this.paths = paths;
        type = Functionality.Type.SRV;
    }

    public Srv(String name, String path, Client service) {
        super(path, service);
        this.name = name;
        type = Functionality.Type.SRV;
    }

    public Srv(String name, Object value, String[] paths) {
        super(name, value);
        this.name = name;
        this.paths = paths;
        type = Functionality.Type.SRV;
    }
    public Srv(String name, Object value) {
        super(name, value);
        this.name = name;
        type = Functionality.Type.SRV;
    }

    public Srv(String name, Object value, ReturnPath returnPath) {
        this(name, value);
        this.returnPath = returnPath;
    }

    public Srv(String name, String path, Object value, ReturnPath returnPath) {
        super(path, value);
        this.returnPath = returnPath;
        this.name = name;
        type = Functionality.Type.SRV;
    }

    public Srv(String name, Object value, String path) {
        super(path, value);
        this.name = name;
    }

    public Srv(String name, Object value, String path, Type type) {
        this(name, value, path);
        this.type = type;
    }

    public Srv(String name, Model model, String path) {
        super(path, model);
        this.name = name;
        type = Functionality.Type.SRV;
    }

    @Override
    public ApplicationDescription getDescription() {
        return null;
    }

    @Override
    public Class<?> getValueType() {
        return null;
    }

    public String[] getPaths() {
        return paths;
    }

    @Override
    public ArgSet getArgs() {
        return null;
    }

    @Override
    public void addArgs(ArgSet set) throws EvaluationException {

    }

    @Override
    public Object getArg(String varName) throws ArgException {
        return null;
    }

    @Override
    public boolean isValueCurrent() {
        return isValid;
    }

    @Override
    public void valueChanged(Object obj) throws EvaluationException {
        srvValue = obj;
    }

    public Mogram exert(Mogram mogram) throws TransactionException, MogramException, RemoteException {
        return exert(mogram, null);
    }

    @Override
    public void valueChanged() throws EvaluationException {
        isValid = false;
    }

    @Override
    public Object getPerturbedValue(String varName) throws EvaluationException, RemoteException {
        return null;
    }

    @Override
    public Object getValue(Arg... entries) throws EvaluationException, RemoteException {
        if (srvValue != null && isValid) {
            return srvValue;
        }
        try {
            substitute(entries);
            if (item instanceof Callable) {
                return ((Callable) item).call();
            } else if (item instanceof SignatureEntry) {
                if (scope != null && scope instanceof SrvModel) {
                    try {
                        return ((SrvModel) scope).evalSignature(((SignatureEntry) item).get(), getKey());
                    } catch (Exception e) {
                        throw new EvaluationException(e);
                    }
                } else if (((SignatureEntry) item).getContext() != null) {
                    try {
                        return execSignature(((SignatureEntry) item).get(),
                                ((SignatureEntry) item).getContext());
                    } catch (MogramException e) {
                        throw new EvaluationException(e);
                    }
                }
                throw new EvaluationException("No model available for entry: " + this);
            } else if (item instanceof MogramEntry) {
                Context cxt = ((MogramEntry) item).get().exert(entries).getContext();
                Object val = cxt.getValue(Context.RETURN);
                if (val != null) {
                    return val;
                } else {
                    return cxt;
                }
            } else if (item instanceof MorphFidelity) {
                return execMorphFidelity((MorphFidelity) item, entries);
            } else {
                return super.getValue(entries);
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    private Object execMorphFidelity(MorphFidelity mFi, Arg... entries)
            throws ServiceException, RemoteException, TransactionException {
        Object obj = mFi.getSelect();
        Object out = null;
        if (obj instanceof Scopable) {
            ((Scopable)obj).setScope(scope);
            isValid(false);
        }
        if (obj instanceof Function) {
            out = ((Function) obj).getValue(entries);
        } else if (obj instanceof Mogram) {
            Context cxt = ((Mogram)obj).exert(entries).getContext();
            Object val = cxt.getValue(Context.RETURN);
            if (val != null) {
                return val;
            } else {
                return cxt;
            }
        }  else if (obj instanceof Service) {
            out = ((Service)obj).execute(entries);
        } else {
            return obj;
        }
        mFi.setChanged();
        mFi.notifyObservers(out);
        return out;
    }

    public Object execSignature(Signature sig, Context scope) throws MogramException {
        ReturnPath rp = (ReturnPath) sig.getReturnPath();
        Path[] ops = null;
        if (rp != null) {
            ops = ((ReturnPath) sig.getReturnPath()).outPaths;
        }
        Context incxt = scope;
        if (sig.getReturnPath() != null) {
            incxt.setReturnPath(sig.getReturnPath());
        }
        Context outcxt = null;
        try {
            outcxt = task(sig, incxt).exert().getContext();
            if (ops != null && ops.length > 0) {
                return outcxt.getDirectionalSubcontext(ops);
            } else if (sig.getReturnPath() != null) {
                return outcxt.getReturnValue();
            }
        } catch (Exception e) {
            throw new MogramException(e);
        }
        if (contextSelector != null) {
            try {
                return contextSelector.doSelect(outcxt);
            } catch (ContextException e) {
                throw new EvaluationException(e);
            }
        } else
            return outcxt;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException, RemoteException {
        Domain mod = Arg.getServiceModel(args);
        if (mod != null) {
            if (mod instanceof SrvModel && item instanceof ValueCallable) {
                return ((ValueCallable) item).call((Context) mod);
            } else if  (mod instanceof Context && item instanceof SignatureEntry) {
                return ((ServiceContext)mod).execSignature(((SignatureEntry) item).get(args));
            } else {
                item = mod;
                return getValue(args);
            }
        }
        return null;
    }

    public Object getSrvValue() {
        return srvValue;
    }

    public void setSrvValue(Object srvValue) {
        this.srvValue = srvValue;
    }

    public ReturnPath getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(ReturnPath returnPath) {
        this.returnPath = returnPath;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getPath() {
        return key;
    }

    @Override
    public double getPerturbation() {
        return 0;
    }

    @Override
    public Signature getSiganture() throws ContextException {
        return srvSignature;
    }

    @Override
    public void setSignature(Signature signature) {
        this.srvSignature = signature;
    }
}
