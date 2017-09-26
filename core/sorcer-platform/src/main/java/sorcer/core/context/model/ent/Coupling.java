package sorcer.core.context.model.ent;

import sorcer.service.Association;

/**
 * Created by Mike Sobolewski on 5/15/17.
 */
public class Coupling extends Association<Tie, Tie> {

    public Coupling(Tie from, Tie to) {
        key = from;
        out = to;
    }

    public Tie from() {
        return key;
    }

    public Tie to() {
        return out;
    }

    public Tie from(Tie from) {
        key = from;
        return key;
    }

    public Tie to(Tie to) {
        item = to;
        return out;
    }

    @Override
    public String toString() {
        return "[" + key + "->" + out + "]";
    }
}
