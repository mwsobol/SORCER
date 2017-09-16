package sorcer.core.context.model.ent;

import sorcer.co.tuple.Tuple2;
import sorcer.service.Entry;

/**
 * Created by Mike Sobolewski on 5/15/17.
 */
public class Tie extends Entry<String, String> {

    public Tie(String domain, String entry) {
        key = domain;
        item = entry;
    }

    public String domain() {
        return key;
    }

    public String entry() {
        return item;
    }


    public String domain(String domain) {
        key = domain;
        return key;
    }

    public String entry(String entry) {
        item = entry;
        return item;
    }

    @Override
    public String toString() {
        return  key + ":" + item;
    }

    @Override
    public int hashCode() {
        int hash = key.length() + key.length() + 1;
        return hash * 31 + key.hashCode() + item.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Tie) {
            if (item != null && ((Tie) object).item == null) {
                return false;
            } else if (item == null && ((Tie) object).item != null) {
                return false;
            } else if (((Tie) object).key.equals(key)
                    && ((Tie) object).item == item) {
                return true;
            } else if (((Tie) object).key.equals(key)
                    && ((Tie) object).item.equals(item)) {
                return true;
            }
        }
        return false;
    }
}
