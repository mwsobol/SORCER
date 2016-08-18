package sorcer.service;

/**
 * Created by Mike Sobolewski on 6/27/16.
 */
public class Fidelity<T> implements Arg, net.jini.core.entry.Entry {
    static final long serialVersionUID = 1L;

    public enum Type implements Arg {
		SELECT, META, NAME, SYS, ENTRY, SIG, CONTEXT, COMPONENT,
		MORPH, MULTI, VAR, REQUEST, UPDATE, ADD, REPLACE, DELETE;

		public String getName() {
			return toString();
		}
	}

	protected static int count = 0;

	protected String name;

	protected String path = "";

	protected T select;

	public Type type = Type.NAME;

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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Fidelity) {
			if (select == null) {
				return path.equals(((Fidelity) obj).getPath())
						&& name.equals(((Fidelity) obj).getName());
			} else {
				return path.equals(((Fidelity) obj).getPath())
						&& name.equals(((Fidelity) obj).getName())
						&& select.equals(((Fidelity) obj).getSelect());
			}
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return (path != null ? path + "@" + name : name);
	}

}
