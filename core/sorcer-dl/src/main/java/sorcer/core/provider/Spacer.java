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

package sorcer.core.provider;

import java.rmi.Remote;

import sorcer.service.Service;

/**
 * SORCER also extends task/job execution abilities through the use of a space
 * rendezvous service implementing the Spacer interface. The Spacer service can
 * drop an exertion into a shared object space implemented in SORCER using
 * JavaSpace in which several providers can retrieve relevant exertions from the
 * object space, execute them, and return the results back to the object space.
 */
public interface Spacer extends Service, Remote {
}
