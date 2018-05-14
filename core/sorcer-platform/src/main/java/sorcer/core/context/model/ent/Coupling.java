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

    public Tie from() {
        return _1;
    }

    public Tie to() {
        return _2;
    }

    public Tie from(Tie from) {
        _1 = from;
        return _1;
    }

    public Tie to(Tie to) {
        _2 = to;
        return _2;
    }

    @Override
    public String toString() {
        return "[" + _1 + "->" + _2 + "]";
    }
}
