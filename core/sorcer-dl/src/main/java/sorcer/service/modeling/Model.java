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
    
    public Context getInputs()  throws ContextException, RemoteException;
    
    public Context getOutputs()  throws ContextException, RemoteException;

    /**
     *  Returns a context od all specified responses of this model. 
     *
     * @param entries  optional configuration arguments
     * @return
     * @throws ContextException
     * @throws RemoteException
     */
    public Context getResponses(Arg... entries)  throws ContextException, RemoteException;

    /**
     * Returns a response for a given <code>path</code>
     *  
     * @param path a path of the response in the model
     * @param entries  optional configuration arguments
     * @return
     * @throws ContextException
     * @throws RemoteException
     */
    public Object getResponse(String path, Arg... entries)  throws ContextException, RemoteException;

    /**
     * Returns a default response of this model
     *
     * @return  a default response
     * @throws ContextException
     * @throws RemoteException
     */
    public Object getResponse() throws ContextException, RemoteException;

}
