package sorcer.core.context.model.ent;

import sorcer.co.tuple.Tuple2;

/**
 * Created by Mike Sobolewski on 5/15/17.
 */
public class Tie extends Tuple2<String, String> {

    private static final long serialVersionUID = 1L;

    public Tie(String domain, String entry) {
        _1 = domain;
        _2 = entry;
    }

    public String domain() {
        return _1;
    }

    public String entry() {
        return _2;
    }


    public String domain(String domain) {
        _1 = domain;
        return _1;
    }

    public String entry(String entry) {
        _2 = entry;
        return _2;
    }

    @Override
    public String toString() {
        return  _1 + ":" + _2;
    }

    @Override
    public int hashCode() {
        int hash = _1.length() + _2.length() + 1;
        return hash * 31 + _1.hashCode() + _2.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Tie) {
            if (_1 != null && ((Tie) object)._2 == null) {
                return false;
            } else if (_2 == null && ((Tie) object)._2 != null) {
                return false;
            } else if (((Tie) object)._1.equals(_1)
                    && ((Tie) object)._2 == _2) {
                return true;
            }
        }
        return false;
    }
}
