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

package sorcer.ui;

import sorcer.service.Context;

public interface ContextHandler {
	public Context getContext();

	// public ServiceContext getContext(String aspect);
	// public void addAppended(String aspect,Object o);
	public void removeContext();// ServiceContext fc);

	public void setContext(Context sc);

	public void addLinkedContext(String name, String selPath, String id,
                                 String isRoot);

	public void appendSubContext(Context sc);

}
