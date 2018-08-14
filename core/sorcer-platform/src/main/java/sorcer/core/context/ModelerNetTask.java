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

package sorcer.core.context;

import sorcer.core.exertion.NetTask;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ModelingTask;

/**
 * The SORCER service modeler net task extending the @link NetTask}.
 * 
 * @author Mike Sobolewski
 */
public class ModelerNetTask extends NetTask implements ModelingTask {

	private static final long serialVersionUID = 1L;

	private ModelTask modelTask;

	public ModelerNetTask() {
		// do nothing
	}

	public ModelerNetTask(String name) {
		super(name);
	}

	public ModelerNetTask(String name, Signature signature)
			throws SignatureException {
		super(name, signature);
	}

	public ModelerNetTask(String name, Signature signature, Context context)
			throws SignatureException {
		super(name, signature, context);
	}
	public ModelerNetTask(Signature signature, Context context)
			throws SignatureException {
		super(signature, context);
	}

	public ModelerNetTask(String name, Signature[] signatures, Context context)
			throws SignatureException {
		super(name, signatures, context);
	}

	public ModelTask getModelTask() {
		return modelTask;
	}

	public void setModelTask(ModelTask modelTask) {
		this.modelTask = modelTask;
	}

	public Model getModel() {
		return modelTask.getModel();
	}

	public Signature getBuilder() throws ContextException {
		return modelTask.getBuilder();
	}

	public ContextSelection getModelSelector() {
		return modelTask.getContextFilter();
	}
}
