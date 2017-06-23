package sorcer.service;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike Sobolewski on 6/27/16.
 */
public class Fidelity<T> implements Fi, Item, Dependency, net.jini.core.entry.Entry {
    static final long serialVersionUID = 1L;

	protected static int count = 0;

	protected String fiName;

	protected String path = "";

	protected T select;

	public Type fiType = Type.SELECT;

	// dependency management for this Fidelity
	protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	public Fidelity() {
		super();
		fiName = "fidelity" + count++;
	}

	public Fidelity(String name) {
		this();
		this.fiName = name;
	}

	public Fidelity(String name, String path) {
		this();
		this.fiName = name;
		this.path = path;
	}

    public Fidelity(String name, String path, String select) {
        this();
        this.fiName = name;
        this.path = path;
        this.select = (T) select;
    }

	@Override
	public String getName() {
		return fiName;
	}

	public void setName(String name) {
		this.fiName = name;
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

	public Type getFiType() {
		return fiType;
	}

	public void setFiType(Type fiType) {
		this.fiType = fiType;
	}

	@Override
	public int hashCode() {
		String id = fiName + path + select;
		int hash = id.length() + 1;
		return hash * 31 + id.hashCode();
	}

	@Override
	public boolean equals(Object object) {

		if(object == this) {
			return true;
		}
		Boolean selectorEquality = true;
		if  (((Fidelity)object).getSelect() != null && select != null)
			selectorEquality = ((Fidelity) object).getSelect().equals(select);


		if (object instanceof Fidelity
				&& ((Fidelity) object).getName().equals(fiName)
				&& ((Fidelity) object).getPath().equals(path)
				&& selectorEquality
				&& ((Fidelity) object).getFiType().equals(fiType)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isValid() {
		//reimplement in subclasses
		return true;
	}

	@Override
	public String toString() {
		return (path == null ? fiName :
				fiName + "@" + path)
                + (select != null ? ":" + select : "")
                + (fiType != null ? ":" + fiType : "");
	}

    @Override
    public Object exec(Arg... args) throws ServiceException, RemoteException {
        if (select instanceof Request) {
            return ((Request)select).exec(args);
        } else {
            return select;
        }
    }

	@Override
	public void addDependers(Evaluation... dependers) {
		if (this.dependers == null)
			this.dependers = new ArrayList<Evaluation>();
		for (Evaluation depender : dependers)
			this.dependers.add(depender);
	}

	@Override
	public List<Evaluation> getDependers() {
		return dependers;
	}
}
