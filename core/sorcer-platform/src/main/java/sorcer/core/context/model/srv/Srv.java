package sorcer.core.context.model.srv;

import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.SignatureEntry;
import sorcer.core.context.ApplicationDescription;
import sorcer.core.context.model.ent.Entry;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Variability;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Callable;

import static sorcer.eo.operator.task;

/**
 * Created by Mike Sobolewski on 4/14/15.
 */
public class Srv extends Entry<Object> implements Variability<Object>, Arg,
        Comparable<Object>, Reactive<Object>, Serializable {

    private static Logger logger = LoggerFactory.getLogger(Srv.class.getName());

    protected final String name;

    protected Object srvValue;

    protected String[] paths;

    protected Signature.ReturnPath returnPath;

    Type type = Type.PAR;;

    // srv fidelities
    protected Map<String, Object> fidelities;

    public Srv(String name) {
        super(name);
        this.name = name;
    }

    public Srv(String name, String path, Type type) {
        super(path, name);
        this.name = name;
        this.type = type;
    }

    public Srv(String name, String path, String[] paths, Service service) {
        super(path, service);
        this.name = name;
        this.paths = paths;
    }

    public Srv(String name, String path, Client service) {
        super(path, service);
        this.name = name;
    }

    public Srv(String path, Object value) {
        super(path, value);
        this.name = path;
    }

    public Srv(String path, Object value, Signature.ReturnPath returnPath) {
        this(path, value);
        this.returnPath = returnPath;
    }

    public Srv(String name, String path, Object value, Signature.ReturnPath returnPath) {
        super(path, value);
        this.returnPath = returnPath;
        this.name = name;
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
    }

    @Override
    public Type getType() {
        return type;
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
        return false;
    }

    @Override
    public void valueChanged(Object obj) throws EvaluationException, RemoteException {

    }

//    @Override
//    public Mogram exert(Mogram mogram, Transaction txn, Arg... args) throws TransactionException, MogramException, RemoteException {
//        return ((Exerter)((SignatureEntry)_2)._2).exert(mogram, txn);
//    }

    public Mogram exert(Mogram mogram) throws TransactionException, MogramException, RemoteException {
        return exert(mogram, null);
    }

    @Override
    public void valueChanged() throws EvaluationException {

    }

    @Override
    public Object getPerturbedValue(String varName) throws EvaluationException, RemoteException {
        return null;
    }

    @Override
    public Object getValue(Arg... entries) throws EvaluationException, RemoteException {
        if (srvValue != null && isValid) {
            return srvValue;
        } else if (_2 instanceof Callable) {
            try {
                return ((Callable) _2).call();
            } catch (Exception e) {
                throw new EvaluationException(e);
            }
        } else if (_2 instanceof SignatureEntry) {
            SrvModel mod = null;
            for (Arg arg : entries) {
                if (arg instanceof SrvModel) {
                    mod = (SrvModel) arg;
                    break;
                }
            }
            if (mod != null) {
                try {
                    return mod.evalSignature(((SignatureEntry)_2)._2, _1);
                } catch (Exception e) {
                    throw new EvaluationException(e);
                }
            } else if (((SignatureEntry)_2).getContext() != null) {
                try {
                    return execSignature(((SignatureEntry)_2)._2,
                            ((SignatureEntry)_2).getContext());
                } catch (MogramException e) {
                    throw new EvaluationException(e);
                }
            }
            throw new EvaluationException("No model available for entry: " + this);
        } else {
            return super.getValue(entries);
        }
    }

    public Object execSignature(Signature sig, Context scope) throws MogramException {
        String[] ips = sig.getReturnPath().inPaths;
        String[] ops = sig.getReturnPath().outPaths;
        Context incxt = scope;
        if (sig.getReturnPath() != null) {
            incxt.setReturnPath(sig.getReturnPath());
        }
        Context outcxt = null;
        try {
            outcxt = ((Task) task(sig, incxt).exert()).getContext();
            if (ops != null && ops.length > 0) {
                return outcxt.getSubcontext(ops);
            } else if (sig.getReturnPath() != null) {
                return outcxt.getReturnValue();
            }
        } catch (Exception e) {
            throw new MogramException(e);
        }
        return outcxt;
    }

    @Override
    public Object exec(Arg... args) throws MogramException, RemoteException {
        Model mod = Arg.getModel(args);
        if (mod != null) {
            if (mod instanceof SrvModel && _2 instanceof ValueCallable) {
                return ((ValueCallable) _2).call((Context) mod);
            } else {
                _2 = mod;
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

    public Signature.ReturnPath getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(Signature.ReturnPath returnPath) {
        this.returnPath = returnPath;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getPath() {
        return _1;
    }

    @Override
    public double getPerturbation() {
        return 0;
    }
}
