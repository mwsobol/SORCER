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
 *  The interface for a service discipline design pattern as governance-multiFi-dispatcher.
 *  Service governance is the indeterminate multifidelity process of decision-making
 *  and the process by which decisions are actualized in the form of a service federation.
 */
public interface Discipline extends Service, Contexting<Object>, Governance {

    /**
     * Returns a context multifidelity
     */
    public ServiceFidelity getContextMultiFi();

    /**
     * Returns a service governance specifying actualization of this discipline
     *
     * @throws ServiceException
     */
    public Service getGovernance() throws ServiceException;

    /**
     * Returns an executed governance of this discipline
     *
     * @throws ServiceException
     */
    public Service getOutGovernance();

    /**
     * Returns a dispatcher multifidelity
     */
    public ServiceFidelity getDispatcherMultiFi();

    /**
     * Returns a dispatcher to govern this discipline
     *
     * @return a dispatcher of this discipline
     * @throws RoutineException
     */
    public Routine getDispatcher() throws RoutineException;

    /**
     * Returns an executed dispatcherof this discipline
     *
     * @return an executed dispatcher of this discipline
     * @throws RoutineException
     */
    public Routine getOutDispatcher();

    /**
     * Returns a service governance multifidelity
     */
    public ServiceFidelity getGovernanceMultiFi();

    /**
     * Returns a discipline input context.
     *
     * @return a current input context
     * @throws ContextException
     */
    public Context getInput() throws ContextException;

    /**
     * Returns an output context of this discipline.
     *
     * @return a current output context
     * @throws ContextException
     */
    public Context getOutput(Arg... args) throws ContextException;

    /**
     * Adds a dispatcher-governance fidelity of this discipline.
     * Fidelity names are names of dispatcher and service correspondingly.
     */
    public void add(Service governance, Routine dispatcher, Context context);

    /**
     * Adds a dispatcher and governance fidelities to this discipline
     */
    public void add(Fidelity governanceFi, Fidelity dispatcherFi, Fidelity contextFi);

    /**
     * Returns a builder of this discipline to be used for replication
     * of this discipline when needed.
     * */
    public Signature getBuilder();
}
