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

package sorcer.core.provider.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sorcer.core.SorcerConstants;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.node.ContextNode;
import sorcer.core.context.node.ContextNodeException;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.util.GenericUtil;
import sorcer.util.Log;
import sorcer.util.Sorcer;

public class Providers implements SorcerConstants {
	private final static Logger logger = Log.getTestLog();

	private Providers() {
		// utility class only
	}

	public static ContextNode[] getContextNodesWithAssoc(Context context,
			String metaAssoc) throws ContextException {
		ContextNode[] ctxtContextNodes = null;
		Vector v = new Vector();
		// Iterate for each element of the context array
		ctxtContextNodes = Contexts.getMarkedConextNodes(context, metaAssoc);
		for (int j = 0; j < ctxtContextNodes.length; j++)
			v.add(ctxtContextNodes[j]);
		ContextNode[] retNodes = new ContextNode[v.size()];
		for (int i = 0; i < v.size(); i++) {
			retNodes[i] = (ContextNode) v.elementAt(i);
		}
		return (retNodes);
	}

	public static ServiceExertion[] getServiceTasks(Job fJ) {
		ServiceExertion[] fTA = new ServiceExertion[fJ.size()];
		for (int i = 0; i < fJ.size(); i++) {
			fTA[i] = (ServiceExertion) fJ.get(i);
		}
		return fTA;
	}

	public static ServiceExertion[] getServiceTasks(Job[] fJA) {
		Vector fTV = new Vector();
		ServiceExertion[] fTA = null;
		for (int i = 0; i < fJA.length; i++) {

			fTA = Providers.getServiceTasks(fJA[i]);

			for (int j = 0; j < fTA.length; j++) {
				fTV.addElement(fTA[j]);
			}
		}
		ServiceExertion[] fTA2 = new ServiceExertion[fTV.size()];
		for (int i = 0; i < fTV.size(); i++) {
			fTA2[i] = (ServiceExertion) fTV.elementAt(i);
		}
		return fTA2;
	}

	public static Context[] getServiceContexts(ServiceExertion[] fTA)
			throws ContextException, MalformedURLException {
		Vector scontexts = new Vector();
		for (int i = 0; i < fTA.length; i++) {
			if (fTA[i].getContext() == null)
				continue;
			scontexts.addElement(fTA[i].getContext());

			// check contexts for SORCER nodes with jobs (recursion)
			if (Providers.hasServiceJob(fTA[i].getContext())) {
				Job[] fJA = null;
				ServiceExertion[] fTA2 = null;

				fJA = Providers.getServiceJobs(fTA[i].getContext());
				fTA2 = Providers.getServiceTasks(fJA);

				Context[] scA = Providers.getServiceContexts(fTA2);

				for (int k = 0; k < scA.length; k++)
					scontexts.addElement(scA[k]);
			}
		}
		Context[] fCA = new Context[scontexts.size()];
		for (int i = 0; i < scontexts.size(); i++) {
			fCA[i] = (Context) scontexts.elementAt(i);
		}
		return fCA;
	}

	public static boolean hasServiceJob(Context sc) throws ContextException {
		ContextNode[] fNA = Contexts.getContextNodes(sc);
		for (int i = 0; i < fNA.length; i++)
			if (Providers.hasServiceJob(fNA[i]))
				return true;
		return false;
	}

	public static boolean hasServiceJob(ContextNode fN) {
		try {
			if ((fN.getData()) instanceof Job) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static Job[] getServiceJobs(Context sc) throws ContextException,
			MalformedURLException {
		if (!(Providers.hasServiceJob(sc)))
			throw new ContextException(
					"No ContextNode with ServiceJob found in context: " + sc);
		ContextNode[] fNA = Contexts.getContextNodes(sc);
		Vector fJV = new Vector();
		for (int i = 0; i < fNA.length; i++) {

			if ((fNA[i].getData()) instanceof Job) {
				fJV.addElement((Job) fNA[i].getData());
			}
		}
		Job[] fJA = new Job[fJV.size()];
		for (int i = 0; i < fJV.size(); i++) {
			fJA[i] = (Job) fJV.elementAt(i);
		}
		return fJA;
	}

	/**
	 * Makes all URL-based context nodes in the <code>context</code> available
	 * as local files in a scratch directory (see <code>sorcer.env</code>
	 * configuration file)
	 * 
	 * @param context
	 * @return the <code>context</code> parameter
	 * @throws ContextNodeException
	 * @throws EvaluationException
	 * @throws IOException
	 */
	public static Context makeUrlContextDataAsLocal(Context context,
			boolean isUniqueName) throws IOException, ContextNodeException,
			EvaluationException {
		File dataDir = Sorcer.getDataDir();
		Iterator it = ((ServiceContext) context).entryIterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			Object val = pair.getValue();
			if (val instanceof ContextNode) {
				String fileName = null;
				ContextNode cn = (ContextNode) val;
				if (cn.isURL()) {
					if (isUniqueName)
						fileName = GenericUtil.getUniqueString() + ".dn";
					else
						fileName = cn.getFile().getName();

					logger.info("making context node data local: "
							+ cn.getData() + ", in: " + dataDir);

					cn.download(new File(dataDir, fileName));
					URL url = Sorcer.getDataURL(new File(dataDir, fileName));
					cn.setValue(url);
					logger.info("made local URL: " + url);
				}
			}
		}
		return context;
	}

	/**
	 * Makes all URL-based context nodes in the <code>context</code> available
	 * as local files in a scratch directory (see <code>sorcer.env</code>
	 * configuration file)
	 * 
	 * @param context
	 * @return the <code>context</code> parameter
	 * @throws ContextNodeException
	 * @throws EvaluationException
	 * @throws IOException
	 */
	public static Context makeUrlContextDataAsScratch(Context context)
			throws IOException, ContextNodeException, EvaluationException {
		File scratchDir = Sorcer.getNewScratchDir();
		Iterator it = ((ServiceContext) context).entryIterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			ContextNode cn = (ContextNode) pair.getValue();
			if (cn.isURL()) {
				String fileName = GenericUtil.getUniqueString() + ".dn";

				logger.info("making context node data local: " + cn.getData()
						+ ", in: " + scratchDir);

				cn.download(new File(scratchDir, fileName));
				cn.setValue(Sorcer
						.getScratchURL(new File(scratchDir, fileName)));
			}
		}
		return context;
	}

}
