package sorcer.core.context.model.ent;

import sorcer.service.Association;

/**
 * Created by Mike Sobolewski on 5/15/17.
 */
public class Coupling extends Association<Tie, Tie> {

    public Coupling(Tie from, Tie to) {
        key = from;
        item = to;
    }

    public Tie from() {
        return key;
    }

    public Tie to() {
        return item;
    }

    public Tie from(Tie from) {
        key = from;
        return key;
    }

    public Tie to(Tie to) {
        item = to;
        return item;
    }

    @Override
    public String toString() {
        return "[" + key + "->" + item + "]";
    }
}
