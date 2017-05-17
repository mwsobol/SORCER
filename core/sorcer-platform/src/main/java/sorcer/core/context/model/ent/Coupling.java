package sorcer.core.context.model.ent;

import sorcer.co.tuple.Tuple2;

/**
 * Created by Mike Sobolewski on 5/15/17.
 */
public class Coupling extends Tuple2<Tie, Tie> {

    public Coupling(Tie from, Tie to) {
        _1 = from;
        _2 = to;
    }

    @Override
    public String toString() {
        return "[" + _1 + "->" + _2 + "]";
    }

    @Override
    public int hashCode() {
        int hash = _1._1.length() + _1._1.length() + 1;
        return hash * 31 + _1._1.hashCode() + _2._2.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Coupling) {
            if (_2 != null && ((Coupling) object)._2 == null) {
                return false;
            } else if (_2 == null && ((Coupling) object)._2 != null) {
                return false;
            } else if (((Coupling) object)._1.equals(_1)
                    && ((Coupling) object)._2 == _2) {
                return true;
            } else if (((Coupling) object)._1.equals(_1)
                    && ((Coupling) object)._2.equals(_2)) {
                return true;
            }
        }
        return false;
    }
}
