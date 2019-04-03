/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.proxy;

import sorcer.service.Service;

import java.rmi.Remote;

/**
 * Remote servers and other compound remote proxies using inner proxies to
 * extend their functionality via calls on the inner proxies implement this
 * interface. To provider service-to-service functionality this interface extends
 * {@link Service}.
 * 
 * @author Mike Sobolewski
 */
public interface RemotePartner extends Partner, Remote {
}
