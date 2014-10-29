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

package sorcer.core.context;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

public class ThrowableTrace implements Serializable {
	private static final long serialVersionUID = 1L;
	public String message;
	public Throwable throwable;
	public String stackTrace;

	public ThrowableTrace(Throwable t) {
		throwable = t;
		stackTrace = getStackTrace(t);
	}

	public ThrowableTrace(String message, Throwable t) {
		this(t);
		this.message = message;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		if (t.getStackTrace() != null)
			t.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}

	public String toString() {
		String info = message != null ? message : throwable.getMessage();
		if (throwable != null)
			return throwable.getClass().getName() + ": " + info;
		else
			return info;
	}

	public String describe() {
		return stackTrace;
	}
}
