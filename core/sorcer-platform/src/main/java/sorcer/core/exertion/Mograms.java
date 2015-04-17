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

package sorcer.core.exertion;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.signature.NetSignature;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

import java.util.*;

public class Mograms implements SorcerConstants {

	private Mograms() {
		// Utility class
	}
	
	public static boolean isCatalogSingleton(Job job) {
		ControlContext cc = job.getControlContext();
		return job.size() == 1
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isCatalogParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isCatalogSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isCatalogBlock(Exertion exertion) {
		ControlContext cc = (ControlContext)exertion.getControlContext();
		return exertion instanceof Block
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isSpaceBlock(Exertion exertion) {
		ControlContext cc = (ControlContext)exertion.getControlContext();
		return exertion instanceof Block
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}
	
	public static boolean isSWIFSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.SWIF.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isSpaceSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isSpaceParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}
	
	public static boolean isSpaceSingleton(Job job) {
		ControlContext cc = job.getControlContext();
		return job.size() == 1
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW));
	}

	public static boolean isSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW));
	}

	public static boolean isMonitorable(Job job) {
		ControlContext cc = job.getControlContext();
		return cc.isMonitorable();
	}

	public static List<Mogram> getInputExertions(Job job) throws ContextException {
		if (job == null || job.size() == 0)
			return null;
		List<Mogram> exertions = new ArrayList<Mogram>();
		Exertion master = job.getMasterExertion();
		for (int i = 0; i < job.size(); i++)
			if (!(job.get(i).equals(master) || job
					.getControlContext().isSkipped(job.get(i))))
				exertions.add(job.get(i));
		return exertions;
	}

	public static ControlContext getCC(Context sc) {
		ControlContext cc = new ControlContext();
		for (Enumeration e = ((Hashtable) sc).keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			cc.put(key, ((Hashtable) sc).get(key));
		}
		cc.setName(sc.getName());
		cc.setId(sc.getId());
		cc.setParentPath(sc.getParentPath());
		cc.setParentID((sc.getParentID() == null) ? null : sc.getParentID());
		cc.setSubject(sc.getSubjectPath(), sc.getSubjectValue());
		cc.setLastUpdateDate(sc.getLastUpdateDate());
		cc.setDescription(sc.getDescription());
		cc.setScopeCode(sc.getScope());
		cc.setOwnerID(sc.getOwnerID());
		cc.setSubjectID(sc.getSubjectID());
		cc.setProject(sc.getProject());
		cc.setAccessClass(sc.getAccessClass());
		cc.setExportControl(sc.getExportControl());
		cc.setGoodUntilDate(sc.getGoodUntilDate());
		cc.setDomainID(sc.getDomainID());
		cc.setSubdomainID(sc.getSubdomainID());
		cc.setDomainName(sc.getDomainName());
		cc.setMetacontext(sc.getMetacontext());
		cc.isPersistantTaskAssociated = ((ServiceContext) sc).isPersistantTaskAssociated;
		return cc;
	}

	public static void removeExceptions(Job job) throws ContextException {
		removeExceptions(job.getContext());
		for (int i = 0; i < job.size(); i++) {
			if (((ServiceExertion) job.get(i)).isJob())
				removeExceptions((Job) job.get(i));
			else
				removeExceptions(((ServiceExertion) job.get(i))
						.getContext());
		}
	}

	public static void removeExceptions(Context sc) {
		for (Enumeration e = ((ServiceContext) sc).keys(); e.hasMoreElements();) {
			String path = (String) e.nextElement();
			if (path.startsWith(SorcerConstants.EXCEPTIONS))
				try {
					sc.removePath(path);
				} catch (Exception ex) {
					// do nothing
				}
		}
		// sc.removeAttribute(SORCER.EXCEPTIONS);
	}

//	public static void preserveNodeReferences(Exertion ref, Exertion res)
//			throws ContextException {
//		if (((ServiceExertion) ref).isJob() && ((ServiceExertion) res).isJob())
//			preserveNodeReferences((Job) ref, (Job) res);
//		else
//			preserveNodeReferences((ServiceExertion) ref, (ServiceExertion) res);
//	}
//
//	public static void preserveNodeReferences(Job refJob,
//			Job resJob) throws ContextException {
//		int size = (refJob.size() < resJob.size()) ? refJob.size() : resJob
//				.size();
//		for (int i = 0; i < size; i++) {
//			if (((ServiceExertion) refJob.get(i)).isJob()
//					&& ((ServiceExertion) resJob.get(i)).isJob())
//				preserveNodeReferences((Job) refJob.get(i),
//						(Job) resJob.get(i));
//			else if (((ServiceExertion) refJob.get(i)).isTask()
//					&& ((ServiceExertion) resJob.get(i)).isTask())
//				preserveNodeReferences(((ServiceExertion) refJob.get(i))
//						.getContext(), ((ServiceExertion) resJob.get(i))
//						.getContext());
//		}
//	}
//
//	public static void preserveNodeReferences(Context refContext,
//			Context resContext) throws ContextException {
//		Contexts.copyContextNodesFrom(resContext, refContext);
//	}

	public static void replaceNullIDs(Exertion ex) throws ContextException {
		if (ex == null)
			return;
		if (((ServiceExertion) ex).isJob()) {
			Job job = (Job) ex;
			if (job.getId() == null)
				job.setId(getId());
			if (job.getContext().getId() == null)
				((ServiceContext)job.getContext()).setId(getId());
			for (int i = 0; i < job.size(); i++)
				replaceNullIDs(job.get(i));
		} else
			replaceNullIDs((ServiceExertion) ex);
	}

	public static void replaceNullIDs(ServiceExertion task) throws ContextException {
		if (task.getId() == null)
			task.setId(getId());
		if (task.getContext() != null) {
			if (task.getContext().getId() == null)
				((ServiceContext)task.getContext()).setId(getId());
		}
	}

	public static synchronized Uuid getId() {
		 return UuidFactory.generate();
	}
	
	public static synchronized String getID() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException ie) {
			// do Nothing
		}
		return Long.toHexString(new Date().getTime());
	}

	public static ExertionEnvelop getEntryEnvelop(Exertion ex)
			throws ExertionException {
		if (ex == null)
			return null;
		else if (ex.getProcessSignature() != null)
			throw new ExertionException("No Method For Exertion e=" + ex);

		ExertionEnvelop eenv = ExertionEnvelop.getTemplate();
		eenv.serviceType = ((NetSignature) ex.getProcessSignature()).getServiceType();
		eenv.providerName = ((NetSignature) ex.getProcessSignature()).getProviderName();
		eenv.exertion = ex;
		eenv.exertionID = ((ServiceExertion)ex).getId();
		eenv.isJob = new Boolean(((ServiceExertion) ex).isJob());
		eenv.state = new Integer(Exec.INITIAL);
		return eenv;
	}

	public static List<Context> getTaskContexts(Exertion ex) throws ContextException {
		List<Context> v = new ArrayList<Context>();
		collectTaskContexts(ex, v);
		return v;
	}

	// For Recursion
	private static void collectTaskContexts(Exertion exertion, List<Context> contexts) throws ContextException {
		if (exertion.isConditional())
			contexts.add(exertion.getDataContext());
		else if (exertion instanceof Job) {
			contexts.add(exertion.getDataContext());
			for (int i = 0; i < ((Job) exertion).getExertions().size(); i++)
				collectTaskContexts(((Job) exertion).get(i),
						contexts);
		} else if (exertion instanceof Task) {
			contexts.add(exertion.getDataContext());
		}
	}
}
