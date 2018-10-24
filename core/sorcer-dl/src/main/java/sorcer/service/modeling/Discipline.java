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

public interface Discipline extends Request {

    /**
     * Returns a service specifying actualization of this discipline
     *
     * @throws ServiceException
     */
    public Service getServer() throws ServiceException;

    /**
     * Returns a server multifidelity
     *
     * @throws ServiceException
     */
    public ServiceFidelity getServerMultiFi() throws ServiceException;

    /**
     * Returns an client to rule this discipline
     *
     * @return a client of this discipline
     * @throws ExertionException
     */
    public Exertion getClient() throws ExertionException;

    /**
     * Returns an exertion multifidelity
     *
     * @throws MogramException
     */
    public ServiceFidelity getClientMultiFi() throws MogramException;

    /**
     * Returns a discipline current input context.
     *
     * @return a current input context
     * @throws ContextException
     */
    public Context getInput() throws ContextException, ExertionException;


    /**
     * Returns a model current output context.
     *
     * @return a current output context
     * @throws ContextException
     */
    public Context getOutput(Arg... args) throws ServiceException;


    /**
     * Returns a buider of this discipline to be used for replication it when needed.
     * */
    public Signature getBuilder();
}
