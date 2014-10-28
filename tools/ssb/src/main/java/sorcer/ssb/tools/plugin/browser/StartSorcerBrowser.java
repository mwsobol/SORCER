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

package sorcer.ssb.tools.plugin.browser;

import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import sorcer.core.SorcerConstants;
import sorcer.ssb.jini.studio.CodeServer;
import sorcer.tools.webster.InternalWebster;
import sorcer.util.Sorcer;

public class StartSorcerBrowser {
	public static boolean isWebsterInt = false;
	
	public static void main(String[] args) {
		System.setSecurityManager(new RMISecurityManager());

		// Initialize system properties: configs/sorcer.env
		Sorcer.getEnvProperties();
		ServiceBrowserUI._logger.info("Provider accessor: " 
				+ Sorcer.getProperty(SorcerConstants.S_SERVICE_ACCESSOR_PROVIDER_NAME));
		
		String val = System.getProperty(SorcerConstants.SORCER_WEBSTER_INTERNAL);
		if (val != null && val.length() != 0) {
			isWebsterInt = val.equals("true");
		}
		String codebase = System.getProperty("java.rmi.server.codebase");
		System.out.println("Using codebase: " + codebase);
		
		if (isWebsterInt) {
			String roots = System.getProperty(SorcerConstants.WEBSTER_ROOTS);
			String[] tokens = null;
			if (roots != null)
				tokens = toArray(roots);
			try {
				InternalWebster.startWebster(tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			if (args.length > 0) {
				final Configuration config = ConfigurationProvider.getInstance(args);
				ServiceBrowserUI._config = config;
				final LoginContext login = (LoginContext) config.getEntry(
						ServiceBrowserUI.CONFIG_MODULE, "loginContext",
						LoginContext.class, null);
				if (login != null) {
					System.out.println("login " + login);
					login.login();
					ServiceBrowserUI.LOGGED_IN = true;
				}
				PrivilegedExceptionAction action = new PrivilegedExceptionAction() {
					public Object run() throws Exception {
						SorcerServiceBrowser.start(null, false, config);
						return null;
					}
				};
				if (login != null) {
					CodeServer.useHttpmd();
					Subject.doAsPrivileged(login.getSubject(), action, null);
				} else {
					action.run();
				}
			} else {
				SorcerServiceBrowser.start(args, false, null);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	private static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}
}
