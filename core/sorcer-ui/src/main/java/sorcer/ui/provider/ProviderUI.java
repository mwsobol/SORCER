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

package sorcer.ui.provider;

import java.awt.GridLayout;
import org.slf4j.Logger;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.jini.core.lookup.ServiceItem;
import sorcer.service.Exerter;
import sorcer.core.proxy.Outer;
import sorcer.util.Log;

/**
 * SORCER provider attribute and context viewer. It also allows to create a
 * service task for its provider and submit the task for the provider execution.
 */
public class ProviderUI extends JPanel {

	private static final Logger logger = Log.getTestLog();

	private static final long serialVersionUID = 1L;

	private ServiceItem item;

	private Exerter provider;

	/** Creates new About ProviderUI */
	public ProviderUI(Object obj) {
		super(new GridLayout(1, 1));
		getAccessibleContext().setAccessibleName("Provider");
		try {
			this.item = (ServiceItem) obj;
			logger.info("ProviderUI>>impl.service:" + item.service);
			if (item.service instanceof Outer) {
				Object inner = ((Outer) item.service).getInner();
				logger.info("ProviderUI>>inner:" + inner);
				// check if smart proxy's inner exported server does contain a
				// provider
				if (inner instanceof Outer)
					provider = (Exerter) ((Outer) inner).getInner();
				else
					// in this case provider contains a non exported server
					provider = (Exerter) inner;
			} else if (item.service instanceof Exerter)
				provider = (Exerter) item.service;

			logger.info("ProviderUI>>provider:" + provider);

			if (provider != null) {
				JTabbedPane tabbedPane = new JTabbedPane();

				tabbedPane.addTab("Attributes", null, createAttributePanel(),
						"Service Provider Attribute Viewer");

				// Add the tabbed pane to this panel.
				add(tabbedPane);

				// Uncomment the following line to use scrolling tabs.
				// tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private JPanel createAttributePanel() {
		return new PropertiesPanel(provider,
				"Service Properties (service proxy, provider, SORCER, and JVM)");
	}
}
