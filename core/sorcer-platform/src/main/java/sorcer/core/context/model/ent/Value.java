package sorcer.core.context.model.ent;

import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Valuation;
import sorcer.service.modeling.val;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.net.URL;

/**
 * @author Mike Sobolewski
 */
public class Value<T> extends Entry<T> implements Valuation<T>, Comparable<T>, Arg, val<T> {

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

    public Value(final String path, final T value) {
        super(path, value);
    }

    public Value(String path, T value, int index) {
        this(path, value);
        this.index = index;
    }

    public Value(String path, T value, boolean isPersistant, int index) {
        this(path, value, index);
        this.isPersistent = isPersistant;
    }

    public void setValue(Object value) throws SetterException {
        if (isPersistent) {
            try {
                if (SdbUtil.isSosURL(value)) {
                    this.item = (T) value;
                } else if (SdbUtil.isSosURL(this.item)) {
                    if (((URL) this.item).getRef() == null) {
                        this.item = (T) SdbUtil.store(value);
                    } else {
                        SdbUtil.update((URL) this.item, value);
                    }
                }
            } catch (MogramException | SignatureException e) {
                throw new SetterException(e);
            }
        } else {
            this.item = (T) value;
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
        return "[" + key + "=" + item + "]";
    }

    @Override
    public T value() {
        return getData();
    }

    public T get(Arg... args) throws ContextException {
        if (args != null && args.length > 0) {
            for (Arg arg : args) {
                if (arg instanceof Fidelity && multiFi != null) {
                    if (((Fidelity) arg).getPath() == null || ((Fidelity) arg).getPath().equals(key)) {
                        multiFi.setSelect(arg.getName());
                        item = (T) ((Value) multiFi.getSelect()).get(args);
                    }
                }
            }
        } else if (item instanceof Entry) {
            return (T) ((Entry)item).getData(args);
        }
        return item;
    }


}
