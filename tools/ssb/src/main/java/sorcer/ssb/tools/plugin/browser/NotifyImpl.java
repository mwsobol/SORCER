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

import java.rmi.Remote;
import java.util.ArrayList;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;

public class NotifyImpl implements RemoteEventListener, Remote, Runnable,
		ServerProxyTrust {

	private RemoteEventListener _impl;
	private ServiceBrowserUI _browserImpl;

	private transient ArrayList _queue = new ArrayList();

	public NotifyImpl(RemoteEventListener impl, ServiceBrowserUI bi) {
		_impl = impl;
		_browserImpl = bi;
		new Thread(_browserImpl.wrap(this)).start();
	}

	public TrustVerifier getProxyVerifier() {
		return _browserImpl.getProxyVerifier();
	}

	public void notify(RemoteEvent theEvent) throws UnknownEventException,
			java.rmi.RemoteException {

		// System.out.println("QUEUE: "+theEvent);

		synchronized (_queue) {
			_queue.add(theEvent);
			_queue.notifyAll();
		}

	}

	public void run() {

		while (true) {
			synchronized (_queue) {

				try {
					if (_queue.size() == 0) {
						_queue.wait();
					}
					_impl.notify((RemoteEvent) _queue.remove(0));

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
