package sorcer.core.invoker;

import sorcer.service.Arg;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ServiceException;
import sorcer.service.Invoker;

import java.rmi.RemoteException;

public class Appender extends Invoker {

    public enum ContextType implements Arg {
        INPUT, SCOPE;

        /* (non-Javadoc)
         * @see sorcer.service.Arg#getName()
         */
        @Override
        public String getName() {
            return toString();
        }

        public Object execute(Arg... args) {
            return this;
        }
    }

    protected Context dataContext;

    protected Context scope;

    protected boolean isNew = false;

    protected ContextType type = ContextType.INPUT;

    public Appender() {
        super(null);
    }

    public Appender(String name) {
        super(name);
    }

    public Appender(String name, Context scope) {
        super(name);
        this.scope = scope;
    }

    @Override
    public Object invoke(Context context, Arg... entries) throws ContextException, RemoteException {
        if (type.equals(ContextType.INPUT)) {
            return dataContext;
        } else if (type.equals(ContextType.SCOPE)) {
            return scope;
        }
        return null;
    }

    @Override
    public Context execute(Arg... args) throws ServiceException, RemoteException {
        if (type.equals(ContextType.INPUT)) {
            return dataContext;
        } else if (type.equals(ContextType.SCOPE)) {
            return scope;
        }
        return null;
    }

    public Context getDataContext() {
        return dataContext;
    }

    public void setDataContext(Context dataContext) {
        this.dataContext = dataContext;
    }

    @Override
    public Context getScope() {
        return scope;
    }

    @Override
    public void setScope(Context scope) {
        this.scope = scope;

    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public ContextType getType() {
        return type;
    }

    public void setType(ContextType type) {
        this.type = type;
    }

}
