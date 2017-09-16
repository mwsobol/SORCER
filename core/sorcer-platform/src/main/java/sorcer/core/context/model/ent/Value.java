package sorcer.core.context.model.ent;

import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Valuation;
import sorcer.util.url.sos.SdbUtil;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 */
public class Value<V> extends Entry<String, V> implements Valuation<V>, Comparable<V>, Identifiable, Arg {

    // sequence number for unnamed mogram instances
    private static int count = 0;

    // its arguments is persisted
    protected boolean isPersistent = false;

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
        if(path == null)
            throw new IllegalArgumentException("path must not be null");
        if (value instanceof ServiceFidelity) {
            multiFi = (ServiceFidelity) value;
            item = (V) ((Value)multiFi.get(0)).getItem();
        } else {
            V v = value;
            if (v == null)
                v = (V) Context.none;
            if (SdbUtil.isSosURL(v)) {
                isPersistent = true;
            }
            this.item = v;
        }
        this.key = path;
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
     * @param isPersistent
     *            the isPersistent to setValue
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

}
