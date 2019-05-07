package sorcer.service;

import sorcer.core.Tag;
import sorcer.service.modeling.fi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike Sobolewski on 6/27/16.
 */
public class Metafidelity extends Fidelity<Fi> implements MetaFi, Dependency, Arg, net.jini.core.entry.Entry, fi<Fi> {

    static final long serialVersionUID = 1L;

	public Metafidelity() {
		super();
		fiName = "fidelity" + count++;
		fiType = Type.META;
	}

	public Metafidelity(String name) {
		this();
		this.fiName = name;
	}

	public Metafidelity(String name, String path) {
		this();
		this.fiName = name;
		this.path = path;
	}

    public Metafidelity(String name, String path, Fi select) {
        this();
        this.fiName = name;
        this.path = path;
        this.select = select;
    }

    public Metafidelity(Fidelity[] fis) {
        fiName = "fidelity" + count++;
        for (Fidelity fi : fis) {
            this.selects.add(fi);
        }
        select = fis[0];
    }

    public Metafidelity(String name, List<Fidelity> fis) {
        for (Fidelity fi : fis) {
            selects.add(fi);
        }
        select = fis.get(0);
        this.fiName = name;
    }

    public Metafidelity(String name, Fidelity[] fis) {
		if (fis.length > 0) {
			for (Fidelity fi : fis) {
				selects.add(fi);
			}
			select = fis[0];
		}
        this.fiName = name;
    }

    public Metafidelity(Type type) {
        this();
        this.fiType = type;
    }

    public void clear() {
        selects.clear();
    }

    @Override
    public Object getId() {
        return getName();
    }

    @Override
	public String getName() {
		return fiName;
	}

	public String getPath() {
		return path;
	}

	public Type getFiType() {
		return fiType;
	}

	public void setType(Type fiType) {
		this.fiType = fiType;
	}

    public Metafidelity(String... selects) {
		this.fiName = "";
		fiType = Type.NAME;
		for (String s : selects)
			this.selects.add((Fi) new Tag(s));
	}

	public Metafidelity(String name, String... selects) {
		this.fiName = name;
		fiType = Type.NAME;
		for (String s : selects)
			this.selects.add((Fi) new Tag(s));
	}


    public String getPath(String fidelityName) {
        for (Fi select : selects) {
            if (select.getName().equals(fidelityName)) {
                if (select instanceof Fidelity) {
                    return ((Fidelity) select).getPath();
                }
            }
        }
        return null;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException, RemoteException {
        if (select instanceof Service) {
            return ((Service)select).execute(args);
        } else return select;
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

		if (object instanceof Fidelity) {
			boolean selectorEquality = true;
			if  (((Fidelity)object).getSelect() != null && select != null)
				selectorEquality = ((Fidelity) object).getSelect().equals(select);

			if (((Fidelity) object).getName().equals(fiName)
					&& ((Fidelity) object).getPath().equals(path)
					&& selectorEquality
					&& ((Fidelity) object).getFiType().equals(fiType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isValid() {
		//reimplement in subclasses
		return true;
	}

	@Override
	public void addDependers(Evaluation... dependers) {
		if (this.dependers == null)
			this.dependers = new ArrayList<Evaluation>();
		for (Evaluation depender : dependers)
			this.dependers.add(depender);
	}

	public Object getOption() {
		return option;
	}

	public void setOption(Object option) {
		this.option = option;
	}

	@Override
	public List<Evaluation> getDependers() {
		return dependers;
	}


}
