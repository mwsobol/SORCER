/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.util;

import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import sorcer.service.Exerter;
import sorcer.service.Accessor;

import java.rmi.RemoteException;

public class ProviderDestroyer {

	public static void main(String... args) throws ClassNotFoundException, ConfigurationException {
		System.setSecurityManager(new SecurityManager());
		// initialize system properties
		Sorcer.getEnvProperties();

		// args: providerName, serviceInfo
		if (args.length == 3) {
			Class serviceType = Class.forName(args[1]);
			Exerter prv = (Exerter) Accessor.create(EmptyConfiguration.INSTANCE).getService(Sorcer.getActualName(args[0]),
                                                                                              serviceType);
			try {
				if (args[2].equals("true")) {
					prv.destroyNode();
				} else {
					prv.destroy();
				}
			} catch (RemoteException e) {
				// ignore it
				//e.printStackTrace();
			}
		}
	}

}
