package sorcer.service;

import sorcer.core.context.ServiceContext;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;
import java.util.List;

public class ServiceDiscipline implements Discipline, Getter<Service> {

    protected ServiceFidelity providerMultiFi;

    protected ServiceFidelity consumerMultiFi;

    protected Context input;

    protected Context output;

    protected Context inConnector;

    protected Context outConnector;

    // the executed consumer
    protected Service out;

    // the service context of the executed consumer
    protected Mogram result;

    protected Signature builder;

    public ServiceDiscipline() {
        // do nothing
    }

    public ServiceDiscipline(Exertion... consumers) {
        consumerMultiFi = new ServiceFidelity(consumers);
    }

    public ServiceDiscipline(Exertion consumer, Service provider) {
        consumerMultiFi = new ServiceFidelity(new Exertion[] { consumer });
        providerMultiFi = new ServiceFidelity(new Service[] { provider });
    }

    public ServiceDiscipline(Exertion[] consumers, Service[] providers) {
        consumerMultiFi = new ServiceFidelity(consumers);
        providerMultiFi = new ServiceFidelity(providers);
    }

    public ServiceDiscipline(List<Exertion> consumers, List<Service> providers) {
        Exertion[] cArray = new Exertion[consumers.size()];
        Service[] pArray = new Exertion[providers.size()];
        consumerMultiFi = new ServiceFidelity(consumers.toArray(cArray));
        providerMultiFi = new ServiceFidelity(providers.toArray(pArray));
    }

    @Override
    public Service getProvider() throws MogramException {
        return providerMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getProviderMultiFi() throws MogramException {
        return providerMultiFi;
    }

    @Override
    public Exertion getConsumer() throws ExertionException {
        return (Exertion) consumerMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getConsumerMultiFi() throws MogramException {
        return consumerMultiFi;
    }

    @Override
    public Context getInput() throws ContextException, ExertionException {
        return getConsumer().getContext();
    }

    @Override
    public Context getOutput(Arg... args) throws ServiceException {
        if (result == null || ! result.isValid()) {
            execute(args);
        }
        if (outConnector != null) {
            if (result instanceof Context) {
                return ((ServiceContext) result).updateContextWith(outConnector);
            } else if (result instanceof Mogram) {
                if (outConnector != null)
                    return ((ServiceContext) result.getContext()).updateContextWith(outConnector);
            }
        } else {
            if (result instanceof Context) {
                return (Context) result;
            } else if (result instanceof Mogram) {
                return result.getContext();
            }
        }
        return output;
    }

    public Mogram getResult() {
        return result;
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
            Exertion xrt = getConsumer();
            if (input != null) {
                if (inConnector != null) {
                    xrt.setContext(((ServiceContext) input).updateContextWith(inConnector));
                } else {
                    xrt.setContext(input);
                }
            }
            getConsumer().setProvider(getProvider());
            return result = getConsumer().exert();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public Service get(Arg... args) throws ContextException {
        return out;
    }
}
