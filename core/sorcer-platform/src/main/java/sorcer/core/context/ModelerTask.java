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

package sorcer.core.context;


import sorcer.core.exertion.ObjectTask;
import sorcer.service.Context;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ModelingTask;

/**
 * The SORCER modeler object task extending the basic task implementation {@link ObjectTask}.
 *
 * @author Mike Sobolewski
 */
public class ModelerTask extends ObjectTask implements ModelingTask {

	static final long serialVersionUID = 1L;

	private ModelTask modelTask;

	public ModelerTask(String name, ModelTask task) {
		super(name);
		modelTask = task;
	}

	public ModelerTask(String name, Signature... signatures) {
		super(name, signatures);
	}

	public ModelerTask(String name, Signature signature, Context context)
			throws SignatureException {
		super(name, signature, context);
	}

	public ModelerTask(Signature signature, Context context)
			throws SignatureException {
		super(signature, context);
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

	public Signature getBuilder() {
		return modelTask.getBuilder();
	}

	public ContextSelection getModelSelector() {
		return modelTask.getContextFilter();
	}
}
