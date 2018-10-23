package sorcer.service;

import net.jini.core.transaction.TransactionException;
import sorcer.service.modeling.Discipline;

import java.rmi.RemoteException;

public class ServiceDiscipline implements Discipline {

    protected ServiceFidelity mogramMultiFi;

    protected ServiceFidelity exertionMultiFi;

    protected Context input;

    protected Context output;

    protected Signature builder;

    @Override
    public Mogram getMogram() throws MogramException {
        return (Mogram) mogramMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getMogramMultiFi() throws MogramException {
        return mogramMultiFi;
    }

    @Override
    public Exertion getExertion() throws ExertionException {
        return (Exertion) exertionMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getExertionMultiFi() throws MogramException {
        return exertionMultiFi;
    }

    @Override
    public Context getInput() throws ContextException, ExertionException {
        return input;
    }

    @Override
    public Context getOutput() throws ContextException {
        return output;
    }

    @Override
    public Signature getBuilder() {
        return builder;
    }

    public void setBuilder(Signature builder) {
        this.builder = builder;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException {
        try {
            output = getExertion().exert().getContext();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
        return output;
    }
}
