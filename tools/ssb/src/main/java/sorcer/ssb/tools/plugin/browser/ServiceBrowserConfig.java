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

import java.io.PrintStream;

import javax.swing.JFrame;

import net.jini.config.Configuration;

public class ServiceBrowserConfig {

	public static String LOG_FILE = "browserLog.txt";

	public static PrintStream sysOut = System.out;

	public static String STATE = ".issb.state";

	public static String BROWSER_HOME;

	public static boolean isPlugin;

	public static JFrame FRAME;
	
	private static Configuration configuration;

	public static Configuration getConfiguration() {
		return configuration;
	}

	public static void setConfiguration(Configuration configuration) {
		ServiceBrowserConfig.configuration = configuration;
	}

}
