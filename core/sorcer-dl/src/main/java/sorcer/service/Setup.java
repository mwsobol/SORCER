/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 12/9/16.
 */
public class Setup implements Serializable {

	private String path;
	private Context context;
	private boolean isValid = true;

	public Setup(String path) {
		this.path = path;
	}

	public Setup(String path, Context context) {
		this.path = path;
		this.context = context;
	}

	public String getName() {
		return path;
	}

	public Context getContext() {
		return context;
	}

	public void setValue(Context value) {
		context = value;
		isValid(false);
	}

	public void setEntry(String path, Object value) throws SetterException {
		try {
			context.putValue(path, value);
		} catch (ContextException e) {
			throw new SetterException(e);
		}
		isValid(false);
	}

	public Object getContextValue(String path) throws ContextException {
		try {
			return context.getValue(path);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
	}

	public boolean isValid() {
		return isValid;
	}

	public void isValid(boolean state) {
		isValid = state;
	}

	@Override
	public String toString () {
		return path+":"+context;
	}

}
