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
package sorcer.service.modeling;

import sorcer.service.*;

/**
 *  The interface for a service discipline design pattern as the triplet: service-fidelity-dispatch
 */
public interface Discipline extends Request {

    /**
     * Returns a service specifying actualization of this discipline
     *
     * @throws ServiceException
     */
    public Service getService() throws ServiceException;

    /**
     * Returns a dispatch multifidelity
     *
     * @throws ServiceException
     */
    public ServiceFidelity getDispatchMultiFi() throws ServiceException;

    /**
     * Returns an dispatch to rule this discipline
     *
     * @return a dispatch of this discipline
     * @throws ExertionException
     */
    public Exertion getDispatch() throws ExertionException;

    /**
     * Returns an service multifidelity
     *
     * @throws MogramException
     */
    public ServiceFidelity getServiceMultiFi() throws MogramException;

    /**
     * Returns a discipline input context.
     *
     * @return a current input context
     * @throws ContextException
     */
    public Context getInput() throws ContextException, ExertionException;

    /**
     * Returns a output of this discipline.
     *
     * @return a current output context
     * @throws ContextException
     */
    public Context getOutput(Arg... args) throws ServiceException;

    /**
     * Adds a dispatch-service fidelity of this discipline.
     * Fidelity names are names of dispatch and service correspondingly.
     */
    public void add(Exertion dispatch, Service service);

    /**
     * Adds a dispatch-service fidelity to this discipline
     */
    public void add(Fidelity dispatchFi, Fidelity serviceFi);

    /**
     * Returns a buider of this discipline to be used for replication it when needed.
     * */
    public Signature getBuilder();
}
