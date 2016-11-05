package sorcer.service;

/**
 * Created by Mike Sobolewski on 6/27/16.
 */
public class Fidelity<T> implements Fi, Arg, net.jini.core.entry.Entry {
    static final long serialVersionUID = 1L;

	protected static int count = 0;

	protected String name;

	protected String path = "";

	protected T select;

	protected String selector;

	public Type type = Type.SELECT;

	public Fidelity() {
		super();
		name = "fidelity" + count++;
	}

	public Fidelity(String name) {
		this();
		this.name = name;
	}

	public Fidelity(String name, String path) {
		this();
		this.name = name;
		this.path = path;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public T getSelect() {
		return select;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setSelect(T select) {
		this.select = select;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getSelector() {
		return selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}

	@Override
	public int hashCode() {
		String id = name + path + selector;
		int hash = id.length() + 1;
		return hash * 31 + id.hashCode();
	}

	@Override
	public boolean equals(Object object) {

        Boolean selectorEquality = true;
       if  (((Fidelity)object).getSelector() != null && selector != null)
           selectorEquality = ((Fidelity) object).getSelector().equals(selector);


		if (object instanceof Fidelity
				&& ((Fidelity) object).getName().equals(name)
				&& ((Fidelity) object).getPath().equals(path)
				&& selectorEquality
				&& ((Fidelity) object).getType().equals(type)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return (path != null ? name + "@" + path : name) + ":" + type;
	}

}
