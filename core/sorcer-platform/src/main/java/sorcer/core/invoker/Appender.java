package sorcer.core.invoker;

import net.jini.core.transaction.Transaction;
import sorcer.service.*;

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
    public Object invoke(Context context, Arg... args) throws ContextException, RemoteException {
        if (type.equals(ContextType.INPUT)) {
            return context.append(dataContext);
        } else if (type.equals(ContextType.SCOPE)) {
            return context.append(scope);
        }
        return context;
    }

    @Override
    public Context execute(Arg... args) throws ServiceException, RemoteException {
        Mogram mog = Arg.selectMogram(args);
        Object out = null;
        if (mog != null) {
            if (mog instanceof Context) {
                return (Context) invoke(((Context) out));
            } else if (mog instanceof  Mogram) {
                out = exert(mog, null);
                return (Context) out;
            } else if (type.equals(ContextType.INPUT)) {
                return dataContext;
            } else if (type.equals(ContextType.SCOPE)) {
                return scope;
            }
        }
        return dataContext;
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


    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... args) throws MogramException, RemoteException {
        Context inContext = null;

        if (mogram !=  null) {
            inContext = mogram.exert(txn, args).getContext();
        }
        if (inContext != null) {
            inContext.append(dataContext);
        } else {
            inContext = dataContext;
        }
        return (T) inContext;
    }
}
