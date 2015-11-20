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

package sorcer.util;

import java.io.Serializable;

import sorcer.security.util.SorcerPrincipal;

/* A Mandate is a serialized command object that contains all
 *  the data required to exert a specific request.
 */
public class Mandate implements Serializable {

	private int commandID;
	private Result results = new Result();
	private Serializable[] args = { "" };
	private SorcerPrincipal principal;

	// constructor - takes an int that represents
	// the type of action that was requested from
	// the client
	public Mandate(int comm) {
		commandID = comm;
	}

	public Mandate(int comm, SorcerPrincipal principal) {
		commandID = comm;
		this.principal = principal;
	}

	// determine which type of action was requested
	public int getCommandID() {
		return commandID;
	}

	// assign any args to pass with the associated command.
	public void setArgs(Serializable[] params) {
		args = params;
	}

	// get any args to pass to the command
	public Serializable[] getArgs() {
		return args;
	}

	public int countArgs() {
		return args.length;
	}

	// get result returned from DB transaction
	public Result getResult() {
		return results;
	}

	public SorcerPrincipal getPrincipal() {
		return principal;
	}

	public void setPrincipal(SorcerPrincipal principal) {
		this.principal = principal;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("Mandate: \n");
		sb.append("commandID=" + commandID + "\n");
		sb.append("results=" + results + "\n");
		sb.append("args=" + SorcerUtil.arrayToString(args) + "\n");
		sb.append("principal=" + principal);
		return sb.toString();
	}

}
