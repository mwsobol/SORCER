package sorcer.core.context.model.ent;

/**
 * Created by Mike Sobolewski on 5/15/17.
 */
public class Tie extends Entry<String> {

    private static final long serialVersionUID = 1L;

    public Tie(String domain, String entry) {
        key = domain;
        out = entry;
    }

    public String domain() {
        return key;
    }

    public String entry() {
        return out;
    }


    public String domain(String domain) {
        key = domain;
        return key;
    }

    public String entry(String entry) {
        item = entry;
        return out;
    }

    @Override
    public String toString() {
        return  key + ":" + out;
    }

    @Override
    public int hashCode() {
        int hash = key.length() + key.length() + 1;
        return hash * 31 + key.hashCode() + item.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Tie) {
            if (item != null && ((Tie) object).out == null) {
                return false;
            } else if (out == null && ((Tie) object).out != null) {
                return false;
            } else if (((Tie) object).key.equals(key)
                    && ((Tie) object).out == out) {
                return true;
            } else if (((Tie) object).key.equals(key)
                    && ((Tie) object).out.equals(out)) {
                return true;
            }
        }
        return false;
    }
}
