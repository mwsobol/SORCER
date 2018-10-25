/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sorcer.service;

import sorcer.core.context.ServiceContext;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Getter;

import java.rmi.RemoteException;
import java.util.List;

public class ServiceDiscipline implements Discipline, Getter<Service> {

    protected String  name;

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

    public void add(Exertion client, Service server) {
        clientMultiFi.getSelects().add(client);
        serverMultiFi.getSelects().add(server);
    }

    @Override
    public void add(Fidelity clientFi, Fidelity serverFi) {
        Exertion client = (Exertion) clientFi.getSelect();
        client.setName(clientFi.getName());
        Object server = serverFi.getSelect();
        if (server instanceof Signature) {
            ((ServiceSignature)server).setName(serverFi.getName());
        } else if (server instanceof Request) {
            ((Request)server).setName(serverFi.getName());
        }
        clientMultiFi.getSelects().add(client);
        serverMultiFi.getSelects().add((Service)server);
    }

    @Override
    public Service getServer() throws MogramException {
        // if no server then client is standalone
        if (serverMultiFi == null || serverMultiFi.getSelect() == null) {
            return  clientMultiFi.getSelect();
        }
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
            List<Fidelity> fis = Arg.selectFidelities(args);
            if (fis != null && fis.size() > 0) {
                selectFi(fis.get(0));
            }
            Exertion xrt = getClient();
            if (input != null) {
                if (inConnector != null) {
                    xrt.setContext(((ServiceContext) input).updateContextWith(inConnector));
                } else {
                    xrt.setContext(input);
                }
            }
            xrt.setProvider(getServer());
            return result = xrt.exert();
        } catch (RemoteException e) {
            throw new ServiceException(e);
        }
    }

    private void selectFi(Fidelity fi) {
        clientMultiFi.selectSelect(fi.getName());
        serverMultiFi.selectSelect(fi.getPath());
    }

    @Override
    public Service get(Arg... args) throws ContextException {
        return out;
    }

    @Override
    public Fi getMultiFi() {
        return clientMultiFi;
    }


    @Override
    public Object getId() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

}
