/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

import sorcer.service.modeling.Data;

import java.rmi.RemoteException;

/**
 * An top-level common interface for all primitive service in SORCER.
 * Actiities are defined by common interface types.
 *
 * @author Mike Sobolewski
 */
public interface Activity extends Service  {

	public Data act(Arg... args) throws ServiceException, RemoteException;

	public Data act(String entryName, Arg... args) throws ServiceException, RemoteException;

}
