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
package sorcer.ssb.tools.plugin.browser;

public class ViewHolder implements java.io.Serializable {

	static final long serialVersionUID = 1;

	public java.awt.Rectangle bounds;
	public String[] filters;
	public boolean isSelected;
	public boolean isDefaultView;
	public String title;
	public boolean logView;
	public boolean multicastView;

	public ViewHolder(String name, java.awt.Rectangle rect, String[] f,
			boolean selected, boolean dv) {
		title = name;
		bounds = rect;
		filters = f;
		isSelected = selected;
		isDefaultView = dv;
	}

}
