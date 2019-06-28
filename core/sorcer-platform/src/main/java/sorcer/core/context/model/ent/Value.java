package sorcer.core.context.model.ent;

import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Valuation;
import sorcer.service.modeling.val;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 */
public class Value<T> extends Entry<T> implements Valuation<T>, Setter, Comparable<T>, Arg, val<T> {

    private static final long serialVersionUID = 1L;

    // sequence number for unnamed mogram instances
    private static int count = 0;

    protected Functionality.Type type = Functionality.Type.VAL;

    protected Class valClass;

    public Value() {
        key = "unknown" + count++;
    }

    public Value(String path) {
        this.key = path;
    }

    public Value(final String path, Object item) {
        super(path, item);
        isValid = true;
    }

    public Value(final String path,  Object item, int index) {
        this(path, item);
        this.index = index;
        isValid = true;
    }

    public Value(final String path, Object item, boolean isPersistant, int index) {
        this(path, item, index);
        this.isPersistent = isPersistant;
        isValid = true;
    }

    public void setValue(Object value) throws SetterException {
        try {
            super.setValue(value);
        } catch (RemoteException e) {
            throw new SetterException(e);
        }
        this.impl = (T) value;
    }

    public T getData(Arg... args) throws ContextException {
        if (out != null) {
            if (multiFi == null) {
                return out;
            } else if (!multiFi.isChanged() && isValid) {
                return out;
            }
        } else {
            out = getValue(args);
            isValid = true;
        }
        return out;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    /* (non-Javadoc)
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
    @Override
    public int compareTo(T o) {
        if (o == null)
            throw new NullPointerException();
        if (o instanceof Value)
            return key.compareTo(((Value) o).getName());
        else
            return -1;
    }

    /**
     * <p>
     * Assigns the flag for persistent storage of values of this entry
     * </p>
     *
     * @param isPersistent the isPersistent to set
     * @return nothing
     */
    public void setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    public Class getValClass() {
        return valClass;
    }

    public void setValClass(Class valClass) {
        this.valClass = valClass;
    }

    public Functionality.Type getType() {
        return type;
    }

    public void setType(Functionality.Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "[" + key + "=" + impl + "]";
    }

    @Override
    public T valuate(Arg... args) throws ContextException {
        return getValue(args);
    }

    public T getValue(Arg... args) throws ContextException {
//        substitute(args);
        if (args != null && args.length > 0) {
            for (Arg arg : args) {
                if (arg instanceof Fidelity && multiFi != null) {
                    if (((Fidelity) arg).getPath() == null || ((Fidelity) arg).getPath().equals(key)) {
                        try {
                            impl = (T) multiFi.selectSelect(arg.getName());
                        } catch (ConfigurationException e) {
                            throw new ContextException(e);
                        }
                        out = (T) ((Slot) impl).getData(args);
                        multiFi.setChanged(false);
                        isValid = true;
                        isChanged = true;
                        isCached = true;
                    }
                }
            }
        }
        if (isPersistent) {
            Object val = impl;
            URL url = null;
            try {
                if (SdbUtil.isSosURL(val)) {
                    val = ((URL) val).getContent();
                    if (val instanceof UuidObject)
                        val = ((UuidObject) val).getObject();
                } else {
                    if (val instanceof UuidObject) {
                        url = SdbUtil.store(val);
                    } else {
                        UuidObject uo = new UuidObject(val);
                        uo.setName(key);
                        url = SdbUtil.store(uo);
                    }
                    impl = (T) url;
                    out = null;
                }
                return (T) val;
            } catch (IOException | MogramException | SignatureException e) {
                throw new ContextException(e);
            }
        } else if (impl instanceof Entry) {
            out = (T) ((Entry) impl).getValue(args);
            isValid = true;
            isChanged = true;
        } else {
            out = (T) impl;
            isValid = true;
            isChanged = true;
        }
        return out;
    }

    public T asis() {
        if (out == null && impl != null) {
            return (T) impl;
        } else {
            return out;
        }
    }
}
