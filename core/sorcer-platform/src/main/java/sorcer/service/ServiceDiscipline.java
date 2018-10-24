package sorcer.service;

import sorcer.core.context.ServiceContext;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;
import java.util.List;

public class ServiceDiscipline implements Discipline, Getter<Service> {

    protected ServiceFidelity serverMultiFi;

    protected ServiceFidelity clientMultiFi;

    protected Context input;

    protected Context output;

    protected Context inConnector;

    protected Context outConnector;

    // the executed client
    protected Service out;

    // the service context of the executed client
    protected Mogram result;

    protected Signature builder;

    public ServiceDiscipline() {
        // do nothing
    }

    public ServiceDiscipline(Exertion... consumers) {
        clientMultiFi = new ServiceFidelity(consumers);
    }

    public ServiceDiscipline(Exertion client, Service server) {
        clientMultiFi = new ServiceFidelity(new Exertion[] { client });
        serverMultiFi = new ServiceFidelity(new Service[] { server });
    }

    public ServiceDiscipline(Exertion[] clients, Service[] servers) {
        clientMultiFi = new ServiceFidelity(clients);
        serverMultiFi = new ServiceFidelity(servers);
    }

    public ServiceDiscipline(List<Exertion> clients, List<Service> servers) {
        Exertion[] cArray = new Exertion[clients.size()];
        Service[] pArray = new Exertion[servers.size()];
        clientMultiFi = new ServiceFidelity(clients.toArray(cArray));
        serverMultiFi = new ServiceFidelity(servers.toArray(pArray));
    }

    @Override
    public Service getServer() throws MogramException {
        return serverMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getServerMultiFi() throws MogramException {
        return serverMultiFi;
    }

    @Override
    public Exertion getClient() throws ExertionException {
        return (Exertion) clientMultiFi.getSelect();
    }

    @Override
    public ServiceFidelity getClientMultiFi() throws MogramException {
        return clientMultiFi;
    }

    @Override
    public Context getInput() throws ContextException, ExertionException {
        return getClient().getContext();
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
            Exertion xrt = getClient();
            if (input != null) {
                if (inConnector != null) {
                    xrt.setContext(((ServiceContext) input).updateContextWith(inConnector));
                } else {
                    xrt.setContext(input);
                }
            }
            getClient().setProvider(getServer());
            return result = getClient().exert();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public Service get(Arg... args) throws ContextException {
        return out;
    }

    @Override
    public Fi getMultiFi() {
        return clientMultiFi;
    }
}
