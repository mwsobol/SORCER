/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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
package sorcer.core.monitor;

import sorcer.service.Exec;
import sorcer.service.Routine;
import sorcer.service.RemoteServiceEvent;

import java.io.Serializable;

/**
 * Monitor Event is passed back by MonitorManager to notify the Service Broker
 * about any monitoring events.
 */
public class MonitorEvent extends RemoteServiceEvent implements Serializable, Exec {
	static final long serialVersionUID = -5433981459997252761L;
	private int cause;
	private Routine ex;

	public MonitorEvent(Object source) {
		super(source);
	}

	public MonitorEvent(Object source, Routine ex, int cause) {
		super(source);
		this.ex = ex;
		this.cause = cause;
	}

	public Routine getExertion() {
		return (ex);
	}

	public int getCause() {
		return (cause);
	}

	public String toString() {
		return " MonitorEvent : Cause =" + cause + " ex =" + ex;
	}

}
