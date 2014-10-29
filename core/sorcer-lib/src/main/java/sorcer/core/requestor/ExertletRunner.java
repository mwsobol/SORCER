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

package sorcer.core.requestor;

import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;

import org.codehaus.groovy.control.CompilationFailedException;

abstract public class ExertletRunner extends ServiceRequestor {

	protected GroovyShell shell;

	public ExertletRunner() {
		// this constructor is required by ModelConsumer
		shell = new GroovyShell();
	}

	public Object evaluate(File scriptFile) throws CompilationFailedException,
			IOException {
		return shell.evaluate(scriptFile);
	}
}
