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
package sorcer.core.provider.logger;

import java.rmi.RemoteException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import sorcer.core.provider.RemoteLogger;
import sorcer.util.ProviderLookup;

/**
 * This class gets a proxy to the Logger and forwards the LogRecord to the
 * logger. For remote logging this handler should be added to the logger created
 * by the class wanting to have its log messages logged remotely.
 */
public class RemoteHandler extends Handler {
	String providerName;
	
	public RemoteHandler() {
		this.setLevel(Level.ALL);
	}

	public RemoteHandler(Level level, String name) {
		this.setLevel(level);
		this.providerName = name;
	}
	
	public void publish(LogRecord record) {
		ClassLoader savedCcl = null;
		Thread t = null;
		try {
			t = Thread.currentThread();
			// get the class loader of the current object to be the thread class
			// loader
			ClassLoader ccl = this.getClass().getClassLoader();

			// save the thread loader to restore later
			savedCcl = t.getContextClassLoader();
			t.setContextClassLoader(ccl);

			RemoteLogger logger = (RemoteLogger) ProviderLookup.getProvider(
					RemoteLogger.class, providerName);
			
			if (isLoggable(record) && logger != null)
				logger.publish(record);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				throw new RemoteException("Failed to get the info of " + RemoteLogger.class.getName(), e);
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		} finally {
			// restore the thread loader
			t.setContextClassLoader(savedCcl);
		}

	}

	public void close() {
	}

	public void flush() {
	}
}
