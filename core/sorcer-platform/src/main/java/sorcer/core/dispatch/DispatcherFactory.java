/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

package sorcer.core.dispatch;

import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Task;

import java.util.Set;

/**
 * This interface must be implemented by all factory classes used to
 * create instances of subclasses of Dispatcher.
 */
public interface DispatcherFactory {
    /**
     * This method returns an instance of the appropriate subclass of
     * Dispatcher as determined from information provided by the given
     * instance of ServiceJob.
     *
     * @param exertion The SORCER job that will be used to perform a collection
     *                 of SERVICE tasks.
     */
	 public Dispatcher createDispatcher(Exertion exertion, Provider provider, String... config) throws DispatcherException;

    public SpaceTaskDispatcher createDispatcher(Task Task, Provider provider, String... config) throws DispatcherException;

    public Dispatcher createDispatcher(Exertion exertion, Set<Context> sharedContexts, boolean isSpawned, Provider provider) throws DispatcherException;
}
