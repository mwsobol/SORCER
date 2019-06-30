package sorcer.service;

import sorcer.service.modeling.Data;
import sorcer.service.modeling.Getter;
import sorcer.service.modeling.slot;
import sorcer.service.modeling.val;

import java.io.Serializable;
import java.rmi.RemoteException;

public class Slot<K, O> implements Identifiable, Data<O>, Arg, slot<O>, net.jini.core.entry.Entry, Serializable {

    protected K key;

    protected O out;

    public Slot() {
    }

    public Slot(K key) {
        this.key = key;
    }

    public Slot(K key, O value) {
        this.key = key;
        this. out = value;
    }

    public K getKey() {
        return key;
    }

    public K key() {
        return key;
    }

    public K path() {
        return key;
    }

    public O out() {
        return out;
    }

    public O getOut() {
        return out;
    }

    public void setOut(O out) {
        this.out = out;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public O getValue(Arg... args) throws ContextException {
        return out;
    }

    public Object asis() throws EvaluationException, RemoteException {
        return out;
    }

    public O valuate(Arg... args) throws ContextException {
        return out;
    }

    public void set(O value) {
        this.out = value;
    }

    @Override
    public Object getId() {
        return key.toString();
    }

    @Override
    public String getName() {
        return key.toString();
    }

    @Override
    public int hashCode() {
        return 2 * 31 + key.hashCode();
    }

    @Override
    public String toString() {
        return "[" + key + ":" + out + "]";
    }

    public boolean equals(Object object) {
        if (object instanceof Slot) {
            if (((Slot) object).key.equals(key)
                    && ((Slot) object).out == out) {
                return true;
            } else  if (((Slot) object).key.equals(key)
                    && ((Slot) object).out.equals(out)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public O getData(Arg... args) throws ContextException {
        return out;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException, RemoteException {
        return out;
    }
}
