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

package sorcer.util;

import java.io.Serializable;
import java.util.Vector;

public class Result extends Vector implements Serializable {
	final public static int EMPTY = 0;

	private int status = -1;

	public Result() {
		super(1, 1);
	}

	public Result(Serializable object) {
		super(1, 1);
		addElement(object);
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int value) {
		status = value;
	}
}
