package sorcer.core.context;

/**
 * Created by Mike Sobolewski on 02/13/15.
 */
public class MapContext extends ServiceContext {

    public enum Direction { IN, OUT }

    public Direction direction;

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
