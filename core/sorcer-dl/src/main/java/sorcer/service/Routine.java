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

package sorcer.service;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import sorcer.core.context.StrategyContext;
import sorcer.core.provider.Jobber;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;
import sorcer.service.modeling.mog;

/**
 * An routine is a procedural service specified by a service requestor -
 * {@link Service#execute}, to a service provider {@link Provider}. It is a
 * form of service-oriented request with a service context
 * {@link sorcer.service.Context} and a collection of service
 * {@link sorcer.service.Signature}s. The service context specifies service data
 * to be processed by the service provider matching routine's signatures. A
 * routine's signature comprises of service operation and a service types
 * implemented by the service provider.<br>
 * Routines are request interseptors in the form of command objects (in the
 * Command programming pattern) that keep a service requestor completely
 * separate from the actions that service providers carry out. Rotines are
 * also completely separate from each other and do not know what other exertions
 * do. However, they can pass parameters via contexts by mapping
 * expected input context values in terms of expected output values calculated
 * by other routines. The operation {@link Routine#exert} of
 * this interface provides for execution of desired actions enclosed in
 * exertions, thus keeping the knowledge of what to do inside of the exertions,
 * instead of having another parts of SO program to make these decisions. When
 * an exertion is invoked then the exertion redirects control to a dynamically
 * bound {@link Provider} matching the exertion's signature of
 * type <code>PROCESS</code>. <br>
 * The <code>Routine</code> interface also provides for the Composite design
 * pattern and defines a common elementary behavior for all exertions of
 * {@link sorcer.service.Task} type and control flow exertions (for branching
 * and looping). A composite exertion called {@link sorcer.service.Job} contains
 * a collection of elementary, flow control, and other composite exertions with
 * the same elementary behavior performed collectively by all its members. Thus
 * {@link sorcer.service.Task}s and control flow exertions are analogous to
 * programming statements and {@link sorcer.service.Job}s analogous to
 * procedures in conventional procedural programming. <br>
 * Control flow exertions allow for branching and looping operations by
 * {@link Provider}s executing exertions. A job combined from
 * tasks and other jobs along with relevant control flow exertions is a
 * service-oriented procedure that can federate its execution with multiple
 * service providers bound dynamically in runtime as determined by
 * {@link Signature}s of job component exertions.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public interface Routine extends Dependency, Invocation<Object>,
		Paradigmatic, Contextion, Serializable, mog {


	/**
	 * Assigns a dispatch for this exertion.
	 */
	public void dispatch(Service service);

	/**
	 * Returns a deployment ID for this exertion.
	 * 
	 * @return a deployment identifier 
	 * @throws NoSuchAlgorithmException 
	 */
	public String getDeploymentId() throws NoSuchAlgorithmException, SignatureException;
	
	/**
	 * Appends the specified component exertion to the end of its list of exertions.
	 * 
	 * @return an added component exertion
	 * @throws ContextException 
	 */
	public Mogram addMogram(Mogram component) throws RoutineException;
	
	/**
	 * Returns a data service context (service data) of this exertion to be
	 * processed by a specified exertion's signature.
	 * 
	 * @return a service context
	 * @throws ContextException 
	 */
	public Context getDataContext() throws ContextException;

	/**
	 * Returns a combined nested data context for parentand
	 * all component exertions of this exertion.
	 * 
	 * @return a service context
	 * @throws ContextException 
	 */
	public Context getContext() throws ContextException;

    public void setContext(Context data);

    /**
	 * Returns a component exertion at a given path.
	 */
	public Mogram getComponentMogram(String path);
	
	/**
	 * Returns a execute associated with a path (key) in this exertion's context.
	 * 
	 * @return a referenced by a path object
	 */
	public Object getValue(String path, Arg... args) throws ContextException;
		
	/**
	 * Returns a return execute associated with a return path in this exertion's context.
	 * 
	 * @return a referenced by a return path object
	 */
	public Object getReturnValue(Arg... entries) throws ContextException,
			RemoteException;
	
	/**
	 * Returns a service context (service data) of the component exertion.
	 * 
	 * @return a service context
	 * @throws ContextException
	 */
	public Context getContext(String componentExertionName) throws ContextException;

	/**
	 * Returns a control context (service control strategy) of this exertion to be 
	 * realized by a tasker, rendezvous or spacer.
	 * 
	 * @return a control context

	 */
	public StrategyContext getControlContext();
	
	public String getExecTime();
	
	/**
	 * Returns a signature of the <code>PROCESS</code> type for this exertion.
	 * 
	 * @return a process service signature
	 */
	public Signature getProcessSignature();

	/**
	 * Returns a flow fiType for this exertion execution. A flow fiType indicates if
	 * this exertion can be executed sequentially, in parallel, or concurrently
	 * with other component exertions within this exertion. The concurrent
	 * execution requires all mapped inputs in the exertion context to be
	 * assigned before this exertion can be executed.
	 * 
	 * @return a flow type
	 * @see {@link Flow}.
	 */
	public Flow getFlowType();

	/**
	 * Returns a provider access type for this exertion execution. An access
	 * type indicates whether the receiving provider specified in the exertion's
	 * process signature is accessed directly (Acess.PUSH) or indirectly
	 * (Acess.PULL) via a shared exertion space.
	 * 
	 * @return an access type
	 * @see {@link Access}.
	 */
	public Access getAccessType();

    // Check if this is a Job that will be performed by Spacer
    boolean isSpacable();

	/**
	 * Returns the list of all signatures of component exertions.
	 * 
	 * @return Signature list
	 */
	public List<Signature> getAllSignatures();

	/**
	 * Returns the list of all net signatures of component exertions.
	 * 
	 * @return Signature list
	 */
	public List<Signature> getAllNetSignatures();
	
	/**
	 * Returns the list of all net task signatures of component exertions.
	 * 
	 * @return Signature list
	 */
	public List<Signature> getAllNetTaskSignatures();

	/**
	 * Returns a component exertion with a given name.
	 * @return Routine list
	 */ 
	public Mogram getMogram(String name);

	/**
	 * Returns the list of direct component exertions.
	 * @return Routine list
	 */ 
	public List<Mogram> getMograms();
	
	/**
	 * Returns the list of all nested component exertions/
	 * @return Routine list
	 */ 
	public List<Mogram> getAllMograms();
	
	/**
	 * Returns <code>true</code> if this exertion can be provisioned for its
	 * execution, otherwise <code>false</code>.
	 * 
	 * @return <code>true</code> if this exertion can be
	 *         provisioned.
	 */
	public boolean isProvisionable();
	
	/**
	 * Returns <code>true</code> if this result exertion should be returned to
	 * its requestor synchronously, otherwise <code>false</code> when accessed
	 * asynchronously.
	 * 
	 * @return <code>true</code> if this exertion is returned synchronously.
	 */
	public boolean isWaitable();
	
	/**
	 * Returns <code>true</code> if this exertion requires execution by a
	 * {@link Jobber}, otherwise <code>false</code> if this exertion requires
	 * its execution by a {@link Tasker}. Note that control follow exertion can be
	 * processed by either a {@link Tasker} or a {@link Jobber}.
	 * 
	 * @return <code>true</code> if this exertion requires execution by a
	 *         {@link Jobber}.
	 */
	public boolean isJob();

	public boolean isTask();
	
	public boolean isBlock();

	public boolean isCmd();
	
	public void setProvisionable(boolean state);

	/**
	 * Returns true if this exertion is atop an acyclic graph in which no node
	 * has two parents (two references to it).
	 * 
	 * @return true if this exertion is atop an acyclic graph in which no node
	 *         has two parents (two references to it).
	 */
	public boolean isTree();
		
	/**
	 * Returns true if this exertion is a branching or looping exertion.
	 */
	public boolean isConditional();
	
	/**
	 * Returns true if this exertion is composed of other exertions.
	 */
	public boolean isCompound();

	class RequestPath<T> implements RoutineRequestPath, Serializable, Arg {
		static final long serialVersionUID = 6158097800741638834L;
		public String returnPath;
		public Signature.Direction direction;
		public Signature.Out outPaths;
		public Path[] inPaths;
		// return value type
		public Class<T> type;
		private Context dataContext;
		public Signature.SessionPaths sessionPaths;
		// out paths used for shared evaluators with RoutineEvaluator
		public Map<String, Signature.Out> evalOutPaths;

		public RequestPath() {
			// return the context
			returnPath = Signature.SELF;
		}

		public RequestPath(String path, Signature.Out argPaths) {
			this.returnPath = path;
			if (argPaths != null && argPaths.size() > 0) {
				Path[] ps = new Path[argPaths.size()];
				this.outPaths = argPaths;
				direction = Signature.Direction.OUT;
			}
		}

		public RequestPath(Path path, Signature.In argPaths) {
			this.returnPath = path.getName();
			if (argPaths != null && argPaths.size() > 0) {
				Path[] ps = new Path[argPaths.size()];
				this.inPaths = argPaths.toArray(ps);
				if (path.direction != null) {
					direction = path.direction;
				} else {
					direction = Signature.Direction.IN;
				}
			}
		}

		public void setInputPaths(Signature.In argPaths) {
			if (argPaths != null && argPaths.size() > 0) {
				Path[] ps = new Path[argPaths.size()];
				this.inPaths = argPaths.toArray(ps);
			}
		}

		public RequestPath(String path, Signature.In argPaths) {
			this.returnPath = path;
			if (argPaths != null && argPaths.size() > 0) {
				Path[] ps = new Path[argPaths.size()];
				this.inPaths = argPaths.toArray(ps);
				direction = Signature.Direction.IN;
			}
		}

		public RequestPath(Signature.Out outPaths) {
			this(null, null, outPaths);
		}

		public RequestPath(Signature.In inPaths) {
			this(null, inPaths, null);
		}

		public RequestPath(Path[] paths) {
			inPaths = paths;
		}

		public RequestPath(String path, Signature.In inPaths, Signature.Out outPaths) {
			this.returnPath = path;
			if (outPaths != null && outPaths.size() > 0) {
				this.outPaths = outPaths;
			}
			if (inPaths != null && inPaths.size() > 0) {
				Path[] ps = new Path[inPaths.size()];
				this.inPaths = inPaths.toArray(ps);
			}
			direction = Signature.Direction.INOUT;
		}

		public RequestPath(String path, Signature.In inPaths, Signature.Out outPaths, Signature.SessionPaths sessionPaths) {
			this(path, inPaths, outPaths);
			this.sessionPaths = sessionPaths;
		}

		public RequestPath(String path, Signature.SessionPaths sessionPaths, Path... argPaths) {
			this(path, argPaths);
			this.sessionPaths = sessionPaths;
		}

		public RequestPath(String path, Path... argPaths) {
			this.returnPath = path;
			if (argPaths != null && argPaths.length > 0) {
				this.outPaths = new Signature.Out(argPaths);
				direction = Signature.Direction.OUT;
			}
		}

		public RequestPath(String path, Signature.Direction direction, Path... argPaths) {
			this.returnPath = path;
			this.outPaths = new Signature.Out(argPaths);
			this.direction = direction;
		}

		public RequestPath(String path, Signature.Direction direction,
                           Class<T> returnType, Path... argPaths) {
			this.returnPath = path;
			this.direction = direction;
			this.outPaths = new Signature.Out(argPaths);
			type = returnType;
		}

		public String getName() {
			return returnPath;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(returnPath != null ? returnPath : "no returnPath");
			if (outPaths != null)
				sb.append("\noutPaths: " + outPaths);
			if (inPaths != null)
				sb.append("\ninPaths: " + inPaths);
			return sb.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			RequestPath that = (RequestPath) o;

			if (!outPaths.equals(that.outPaths)) return false;
			if (direction != that.direction) return false;
			if (!returnPath.equals(that.returnPath)) return false;
			if (type != null ? !type.equals(that.type) : that.type != null) return false;

			return true;
		}

		public Context getDataContext() {
			return dataContext;
		}

		public void setDataContext(Context dataContext) {
			this.dataContext = dataContext;
		}

		@Override
		public int hashCode() {
			int result = returnPath.hashCode();
			result = 31 * result + (direction != null ? direction.hashCode() : 0);
			result = 31 * result + (outPaths != null ? outPaths.hashCode() : 0);
			result = 31 * result + (inPaths != null ? outPaths.hashCode() : 0);
			result = 31 * result + (type != null ? type.hashCode() : 0);
			return result;
		}

		public static String[] getPaths(Path[] paths) {
			String[] ps = new String[paths.length];
			for (int i = 0; i < paths.length; i++) {
				ps[i] = paths[i].path;
			}
			return ps;
		}

		public Path[] getInSigPaths() {
			return inPaths;
		}


		public Signature.Out getOutSigPaths() {
			return outPaths;
		}

		@Override
		public String getReturnPath() {
			return returnPath;
		}

		@Override
		public void setReturnPath(String path) {
			this.returnPath = path;
		}

		@Override
		public String[] getInPaths() {
			if (inPaths != null)
				return getPaths(inPaths);
			else
				return null;
		}

		@Override
		public Signature.Out getOutPaths() {
			if (outPaths != null)
				return outPaths;
			else
				return null;
		}

		public Object execute(Arg... args) {
			return this;
		}
	}
}