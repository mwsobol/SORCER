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

package sorcer.core.monitor;

import java.io.Serializable;

import sorcer.service.Exec;
import sorcer.service.Exertion;
import sorcer.service.RemoteServiceEvent;

/**
 * Monitor Event is passed back by MonitorManager to notify the Service Broker
 * about any monitoring events.
 */
public class MonitorEvent extends RemoteServiceEvent implements Serializable,
		Exec {

	static final long serialVersionUID = -5433981459997252761L;
	public static final long ID = 9999699999899L;
	private int cause;
	private Exertion ex;

	public MonitorEvent(Object source) {
		super(source);
	}

	public MonitorEvent(Object source, Exertion ex, int cause) {
		super(source);
		this.ex = ex;
		this.cause = cause;
	}

	public Exertion getExertion() {
		return (ex);
	}

	public int getCause() {
		return (cause);
	}

	public String toString() {
		return " MonitorEvent : Cause =" + cause + " ex =" + ex;
	}

}
