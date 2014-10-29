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

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import sorcer.ssb.jini.studio.CodeServer;

public class StartBrowser {
	public static void main(String[] args) {
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
						ServiceBrowser.start(null, false, config);
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
				ServiceBrowser.start(args, false, null);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
