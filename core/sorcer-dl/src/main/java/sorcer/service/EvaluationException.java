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

import sorcer.core.context.ThrowableTrace;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvaluationException extends ContextException {
	private static final long serialVersionUID = 5987271488623420213L;
	private List<ThrowableTrace> throwableTraces = new ArrayList<>();

	public EvaluationException() {
	}

	public EvaluationException(Exception exception) {
		super(exception);
	}
	
	public EvaluationException(String msg, Exception e) {
		super(msg, e);
	}

	public EvaluationException(String msg) {
		super(msg);
	}

    public EvaluationException(String msg, List<ThrowableTrace> throwableTraces) {
	    super(msg);
        this.throwableTraces.addAll(throwableTraces);
    }

    @Override public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        for(ThrowableTrace t : throwableTraces)
            s.print(t.printStackTrace());
    }

    @Override public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        for(ThrowableTrace t : throwableTraces)
            s.print(t.printStackTrace());
    }

    @Override public StackTraceElement[] getStackTrace() {
	    List<StackTraceElement> stackTrace = new ArrayList<>();
        Collections.addAll(stackTrace, super.getStackTrace());
        for(ThrowableTrace t : throwableTraces)
            Collections.addAll(stackTrace, t.getStackTrace());
        return stackTrace.toArray(new StackTraceElement[0]);
    }
}
