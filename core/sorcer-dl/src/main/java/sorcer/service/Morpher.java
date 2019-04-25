/*
* Copyright 2015 SORCERsoft.org.
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

import sorcer.service.Arg;
import sorcer.service.FidelityManagement;
import sorcer.service.Service;
import sorcer.service.ServiceException;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 11/9/15.
 */
@FunctionalInterface
public interface Morpher<T extends Service> {

    public void morph(FidelityManagement<T> manager, Fi<T> mFi, Object value) throws RemoteException, ServiceException, ConfigurationException;
}
