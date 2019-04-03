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

/*
 *
 * Created Thu Oct 07 13:15:13 BST 2004
 */

import javax.swing.JFrame;

public class BrowserFrame extends JFrame {

	private FiltersView _filtersView;
	private boolean _isDefault;
	private ServiceBrowserUI _browser;

	public BrowserFrame(String title) {
		super(title);
		setIconImage(TreeRenderer._frameIcon.getImage());
	}

	public BrowserFrame(String title, boolean dfv) {
		this(title);
		_isDefault = dfv;
	}

	public void setFiltersView(FiltersView view) {
		_filtersView = view;
	}

	public FiltersView getFiltersView() {
		return _filtersView;
	}

	public String[] getFilters() {
		return _filtersView.getAllText();
	}

	public boolean isDefault() {
		return _isDefault;
	}

	public void setBrowser(ServiceBrowserUI browser) {
		_browser = browser;
	}

	public void terminate() {
		_browser.blockingTerminate();
	}

}
