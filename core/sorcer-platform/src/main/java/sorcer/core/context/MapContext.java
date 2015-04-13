package sorcer.core.context;

import net.jini.id.UuidFactory;
import sorcer.service.Arg;

import java.util.Hashtable;

/**
 * Created by Mike Sobolewski on 02/13/15.
 */
public class MapContext extends ServiceContext {

    public MapContext() {
        super();
    }

    /**
     * Constructor for a named instance of MapContext. It calls on the default constructor
     * @param name
     * @see ServiceContext
     */
    public MapContext(String name) {
        super(name);
    }
}
