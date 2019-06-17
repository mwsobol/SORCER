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

package sorcer.service.modeling;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import sorcer.service.*;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;

public interface Modeling extends Model {
	
	public EventRegistration register(long eventID, MarshalledObject handback,
			RemoteEventListener toInform, long leaseLenght)
			throws UnknownEventException, RemoteException;

	public void deregister(long eventID) throws
			UnknownEventException, RemoteException;

	public void notifyEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public void notifyConfigureEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public void notifySelectEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public void notifyUpdateEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public Context setInputs(Context entries)
			throws ContextException, RemoteException;

	public void evaluate(Fidelity... fidelities)
			throws EvaluationException, RemoteException;

	/**
	 * Returns a execute at the path.
	 *
	 * @param path
	 *            the attribute-based path
	 * @return this model execute
	 * @throws ContextException
	 */
	public Object getValue(String path, Arg... args)
			throws ContextException, RemoteException;

	public Context configureEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public Context selectEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public Context updateEvaluation(Context modelContext)
			throws EvaluationException, RemoteException;

	public boolean writeResult() throws
			EvaluationException, RemoteException;
	
	public void setContext(Context context)
			throws ContextException;

	public void reconfigure(Fidelity... fidelities)
			throws ConfigurationException, RemoteException;

	public void isolateModel(Context inContext)
			throws ContextException;
	
	public void initializeBuilder()
			throws ContextException;
	
	public static enum Type {
		RESPONSE, PARAMETRIC, OPTIMIZATION
	}

	public static enum ParType {
		THREAD, SERVICE
	}
	
	public static String IN_TABLE = "table/in";
	
	public static String IN_TABLE_RANGE = "table/inputs/range";
	
	public static String OUT_TABLE = "table/out";
	
	public static String IN_STREAM = "table/instream";
	
	public static String OUT_STREAM = "table/outstream";

	public static String IN_TABLE_RESOURCE = "table/in/resource";
	
	public static String OUT_TABLE_RESOURCE = "table/out/resource";

	public static String MODEL_STRATEGY = "model/strategy";

	// fidelity manger configuration data
	public static final String MODEL_MORPHERS = "model/fidelity/morhers";


}
