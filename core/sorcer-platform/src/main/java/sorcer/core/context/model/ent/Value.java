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
public class Value<V> extends Entry<V> implements Valuation<V>, Comparable<V>, Identifiable, Arg, val<V> {

    // sequence number for unnamed mogram instances
    private static int count = 0;

    protected Functionality.Type type = Functionality.Type.VAL;

    protected Class valClass;

    protected ServiceFidelity multiFi;

    public Value() {
        key = "unknown" + count++;
    }

    public Value(String path) {
        this.key = path;
    }

    public Value(final String path, final V value) {
        super(path, value);
        if (value instanceof ServiceFidelity) {
            multiFi = (ServiceFidelity) value;
            item = (V) ((Value) multiFi.get(0)).getItem();
        }
    }

    public Value(String path, V value, int index) {
        this.key = path;
        this.item = value;
        this.index = index;
    }

    public Value(String path, V value, boolean isPersistant, int index) {
        this(path, value, index);
        this.isPersistent = isPersistant;
    }

    public void setValue(Object value) throws SetterException {
        if (isPersistent) {
            try {
                if (SdbUtil.isSosURL(value)) {
                    this.item = (V) value;
                } else if (SdbUtil.isSosURL(this.item)) {
                    if (((URL) this.item).getRef() == null) {
                        this.item = (V) SdbUtil.store(value);
                    } else {
                        SdbUtil.update((URL) this.item, value);
                    }
                }
            } catch (MogramException | SignatureException e) {
                throw new SetterException(e);
            }
        } else {
            this.item = (V) value;
        }
    }

    public Object getId() {
        return toString();
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public ServiceFidelity getMultiFi() {
        return multiFi;
    }

    public void setMultiFi(ServiceFidelity multiFi) {
        this.multiFi = multiFi;
    }

    /* (non-Javadoc)
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
    @Override
    public int compareTo(V o) {
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
    public V value() {
        return getData();
    }

    public V get(Arg... args) throws ContextException {
        if (args != null && args.length > 0) {
            for (Arg arg : args) {
                if (arg instanceof Fidelity && multiFi != null) {
                    if (((Fidelity) arg).getPath() == null || ((Fidelity) arg).getPath().equals(key)) {
                        multiFi.setSelect(arg.getName());
                        item = (V) ((Value) multiFi.getSelect()).get(args);
                    }
                }
            }
        } else if (item instanceof Entry) {
            return (V) ((Entry)item).getData(args);
        }
        return item;
    }


}
