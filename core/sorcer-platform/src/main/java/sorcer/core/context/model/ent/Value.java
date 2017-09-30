package sorcer.core.context.model.ent;

import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Valuation;
import sorcer.service.modeling.val;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.net.URL;

/**
 * @author Mike Sobolewski
 */
public class Value<T> extends Entry<T> implements Valuation<T>, Comparable<T>, Arg, val<T> {

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
    }

    public Value(final String path,  Object item, int index) {
        this(path, item);
        this.index = index;
    }

    public Value(final String path, Object item, boolean isPersistant, int index) {
        this(path, item, index);
        this.isPersistent = isPersistant;
    }

    public void setValue(Object value) throws SetterException {
        if (isPersistent) {
            try {
                if (SdbUtil.isSosURL(value)) {
                    this.out = (T) value;
                    this.impl = value;
                } else if (SdbUtil.isSosURL(this.impl)) {
                    if (((URL) this.impl).getRef() == null) {
                        this.impl = (T) SdbUtil.store(value);
                    } else {
                        SdbUtil.update((URL) this.impl, value);
                    }
                }
            } catch (MogramException | SignatureException e) {
                throw new SetterException(e);
            }
        } else {
            this.out = (T) value;
            this.impl = (T) value;
        }
    }

    public Object getId() {
        return toString();
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
    public T valuate() throws ContextException {
        return getData();
    }

    public T get(Arg... args) throws ContextException {
//        substitute(args);
        if (args != null && args.length > 0) {
            for (Arg arg : args) {
                if (arg instanceof Fidelity && multiFi != null) {
                    if (((Fidelity) arg).getPath() == null || ((Fidelity) arg).getPath().equals(key)) {
                        impl = multiFi.setSelect(arg.getName());
                        out = (T) ((Entry) impl).getData(args);
                        multiFi.setChanged(false);
                        isValid = true;
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
                    impl = url;
                    out = null;
                }
                return (T) val;
            } catch (IOException | MogramException | SignatureException e) {
                throw new ContextException(e);
            }
        } else if (impl instanceof Entry) {
            out = (T) ((Entry) impl).get(args);
            isValid = true;
        } else {
            out = (T) impl;
            isValid = true;
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
