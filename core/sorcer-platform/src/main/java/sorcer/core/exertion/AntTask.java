/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

import java.io.File;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import net.jini.core.transaction.Transaction;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import sorcer.core.signature.AntSignature;
import sorcer.service.Context;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.util.SorcerUtil;

/**
 * * The SORCER ant task extending the basic task implementation {@link Task}.
 * 
 * @author Mike Sobolewski
 */
public class AntTask extends Task {

	private static final long serialVersionUID = 4825649268789975278L;

	private Project project;

	public AntTask(String name) {
		super(name);
	}

	public AntTask(String name, AntSignature signature) {
		super(name);
		addSignature(signature);
	}

	public AntTask(AntSignature signature) {
		this(null, signature, null);
	}

	public AntTask(AntSignature signature, Context context) {
		this(null, signature, context);
	}

	public AntTask(String name, AntSignature signature, Context context) {
		super(name);
		addSignature(signature);
		if (context != null)
			setContext(context);
	}

	public Project getAntProject() {
		return project;
	}

	public void setAntProject(Project project) {
		this.project = project;
	}
	
	@Override
	public Task doTask(Transaction txn) throws ExertionException,
			SignatureException, RemoteException {
		File antFile = ((AntSignature) getProcessSignature()).getBuildFile();
		String target = getProcessSignature().getSelector();
		project = new Project();
		project.init();
		ProjectHelper.configureProject(project, antFile);

		try {
			String name = (String)dataContext.getSoftValue("name");
			if (name != null && name != Context.none)
				project.setName((String) dataContext.getSoftValue("name"));
			String targetsToRun = (String)dataContext.getSoftValue("targets");

			if (targetsToRun != null && targetsToRun != Context.none) {
				String[] targetArrray = SorcerUtil.getTokens(targetsToRun, ", ");
				Vector<String> targets;
				if (targetArrray.length > 0) {
					targets = new Vector<String>(targetArrray.length + 1);
					targets.add(target);
					for (String t : targetArrray) {
						targets.add(t);
					}
					project.executeTargets(targets);
				}
			} else {
				project.executeTarget(target);
			}
			// append properties of the project
			Hashtable props = project.getProperties();
			Hashtable uprops = project.getUserProperties();
			dataContext.putAll(props);
			dataContext.putAll(uprops);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return this;
	}

}
