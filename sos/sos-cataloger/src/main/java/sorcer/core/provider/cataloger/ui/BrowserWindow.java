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

package sorcer.core.provider.cataloger.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Observable;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * BrowserWindow is the main UI component of CatalogerUI and the providers
 * integrated Task Manager Tab. It displays the provider, interface,
 * method(SignatureView), and the context(ContextView) in a JSplitPane.
 */
public class BrowserWindow extends JSplitPane {
	private static final long serialVersionUID = -7979919060009568791L;

	/**
	 * Creates the BrowserWindow from for the Cataloger service or Task Editor
	 * <p>
	 * 
	 * @param model
	 * @link BrowserModel the data will be stored in
	 * @param dispatcher
	 *            Dispatcher that should be utilized.
	 */
	public BrowserWindow(BrowserModel model, SignatureDispatchment dispatcher,
			boolean withProviderList) {
		super();
		// combines the signature and context panes.
		SignatureView sigPane = new SignatureView(withProviderList);
		sigPane.addListeners(dispatcher, dispatcher.getProviderListener(),
				dispatcher.getInterfaceListener(), dispatcher
						.getMethodListener());

		//ContextView contextPane = new ContextView(dispatcher);
		
		ContextScriptView scriptPane = new ContextScriptView(dispatcher, model);
		scriptPane.setMinimumSize(new Dimension(300, 300));
//		JPanel panel = new JPanel();
//		panel.setLayout(new BorderLayout());
//		panel.add(scriptPane, BorderLayout.CENTER);
//		//panel.add(contextPane, BorderLayout.CENTER);
//		panel.setMinimumSize(new Dimension(400, 300));

		model.addObserver(sigPane);
//		if (contextPane != null)
//			model.addObserver(contextPane);

		this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		this.setLeftComponent(sigPane);
		this.setRightComponent(scriptPane);
		this.setPreferredSize(new Dimension(600, 400));
		this.setOneTouchExpandable(true);
		this.setContinuousLayout(true);
		this.setResizeWeight(0.4);
	}
	
}