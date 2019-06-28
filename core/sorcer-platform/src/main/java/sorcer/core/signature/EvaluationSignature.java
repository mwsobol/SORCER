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

package sorcer.core.signature;

import sorcer.service.Evaluation;
import sorcer.service.modeling.sig;

public class EvaluationSignature extends ServiceSignature implements sig {

	static final long serialVersionUID = -7142818720710405999L;
	
	private Evaluation evaluator;

	public EvaluationSignature(Evaluation evaluator) {
		this.evaluator = evaluator;
	}

	public EvaluationSignature(String name, Evaluation evaluator) {
		this.evaluator = evaluator;
		evaluator.setName(name);
	}

	/**
	    <p> Returns the evaluator for this signature. </p>
	   
	    @return the evaluator
	 */
	public Evaluation getEvaluator() {
		return evaluator;
	}

	/**   
	    <p> Sets the evaluator for this signature. </p>
	
	    @param evaluator the evaluation to set
	 */
	public void setEvaluator(Evaluation evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean equals(Object signature) {
		if (!(signature instanceof EvaluationSignature))
			return false;
		return ("" + ((EvaluationSignature) signature).evaluator).equals(""
				+ evaluator);

	}

	@Override
	public int hashCode() {
		return 31 * toString().hashCode();
	}
	
	public String toString() {
		return this.getClass() + ":" + providerName + ";" + execType + ";"
				+ isActive + ";" + evaluator;
	}
}
