/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

import java.rmi.RemoteException;

/**
 * An top-level common interface for all service Models in SORCER.
 *
 * @author Mike Sobolewski
 */
public interface Model extends Mogram, Dependency {


    /**
     *  Returns a model specific value representing the main model goal.
     *
     * @return  a domain specific result
     * @throws ContextException
     * @throws RemoteException
     */
    public Object getResult() throws ContextException, RemoteException;


    /**
     *  Returns the context of all current outputs of this model with a provided configuration.
     *
     * @param args optional configuration arguments
     * @return
     * @throws ContextException
     * @throws RemoteException
     */
    public Object getResponse(Arg... args)  throws ContextException, RemoteException;

    /**
     *  Returns a requested evaluation context with a provided input context.
     *
     * @param inputContext  a context for this evaluation
     * @return the result context of this evaluation
     * @throws ContextException
     * @throws RemoteException
     */
    public Context evaluate(Context inputContext)  throws ContextException, RemoteException;

    /**
     * Returns the input context of this model.
     *
     * @return the input context
     * @throws ContextException
     * @throws RemoteException
     */
    public Context getInputs()  throws ContextException, RemoteException;

    /**
     *  Returns the output context of this model.
     *
     * @return  the output context
     * @throws ContextException
     * @throws RemoteException
     */
    public Context getOutputs()  throws ContextException, RemoteException;

    /**
     * Returns a default response of this model
     *
     * @param responseName a response name of the model
     * @throws ContextException
     * @throws RemoteException
     */
    public void addResponse(String responseName) throws ContextException, RemoteException;

    /**
     *  Returns a inConnector as a map of input paths of tis model mapped to output paths of the sender.
     *  A inConnector specifies a map of an input context of this model.
     *
     * @param args  optional configuration arguments
     * @return
     * @throws ContextException
     * @throws RemoteException
     */
    public Context getInConnector(Arg... args)  throws ContextException, RemoteException;

    /**
     *  Returns a inConnector as a map of input paths of the receiver to output path of this model.
     *  A inConnector specifies a map of an input context as needed by another collaborating service.
     *
     * @param args  optional configuration arguments
     * @return                                                                                  s
     * @throws ContextException
     * @throws RemoteException
     */

    public Context getOutConnector(Arg... args)  throws ContextException, RemoteException;

}
