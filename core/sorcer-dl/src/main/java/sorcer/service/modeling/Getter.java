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

import sorcer.service.Arg;
import sorcer.service.ContextException;

import java.rmi.RemoteException;


/**
 * A getter is a generic agent that returnsa calculated in  valuate. A
 * getter may evaluate provided arguments and return a calculated execute
 * by itself or retrieve execute from a data repository. Alternatively a getter
 * may use a target as an independent agent at which the getting operation is
 * aimed (the third party service) to obtain the dependent executtion, or finally
 * as a filter that can filter out the result from the provided target.
 *
 * @author Mike Sobolewski
 */
public interface Getter<T>  {

    public T get(Arg... args) throws ContextException;
}
