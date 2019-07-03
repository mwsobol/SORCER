package sorcer.service;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

abstract public class Invoker<T> implements Invocation<T>, Exertion, Scopable, Opservice, Identifiable {

    private static final long serialVersionUID = 1L;

    public Invoker() {
        this(null);
    }

    public Invoker(String name) {
        if (name == null)
            this.name = defaultName + count++;
        else
            this.name = name;
    }

    protected String name;

    protected String defaultName = "invoker-";

    protected Uuid id = UuidFactory.generate();

    // counter for unnamed instances
    protected static int count;

    protected Fi multiFi;

    protected Morpher morpher;

    @Override
    public Fi getMultiFi() {
        return multiFi;
    }

    @Override
    public Morpher getMorpher() {
        return morpher;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
