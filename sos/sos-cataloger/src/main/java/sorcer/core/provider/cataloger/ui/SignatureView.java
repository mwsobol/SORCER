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
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 * Builds the provider, interface, and method list, and puts them into a JPanel.
 */
public class SignatureView extends JPanel implements Observer {
	private static final long serialVersionUID = -6644789175908559332L;

	/**
	 * Provider, interface and method lists which are displayed on the screen.
	 */
	public JList provider_l, interface_l, method_l;
	private JScrollPane provider_s, interface_s, method_s;
	private JTextField provider_t;
	// interface_t, method_t;
	private JPanel provider_p;
	private JSplitPane interface_p;
	private boolean withProviderList = true;
	// method_p;
	public static final String PROVIDER_SEARCH = "ProviderSearch",
			INTERFACE_SEARCH = "IntSearch", METHOD_SEARCH = "MethodSearch";
	/**
	 * Creates a panel that contains the Provider, method, and interface lists,
	 * as well as search bars for each list.
	 */
	private DefaultListModel listModel;

	/**
	 * Constructor used to create a signature view.
	 */
	public SignatureView(boolean withProviderList) {
		super();
		this.withProviderList = withProviderList;
		createSignatureVew();
	}

	private void createSignatureVew() {
		// mdl=(BrowserModel)m;
		// start off by building the lists
		listModel = new DefaultListModel();
		provider_l = new JList(listModel);
		interface_l = new JList();
		// interface_l.setVisibleRowCount(3);
		method_l = new JList();
		// method_l.setVisibleRowCount(5);

		// make them pretty
		provider_l.setBorder(BorderFactory.createLoweredBevelBorder());
		interface_l.setBorder(BorderFactory.createLoweredBevelBorder());
		method_l.setBorder(BorderFactory.createLoweredBevelBorder());
		provider_l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		interface_l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		method_l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// toss them into scroll panes
		provider_s = new JScrollPane(provider_l);
		provider_s.setPreferredSize(new Dimension(100, 400));
		interface_s = new JScrollPane(interface_l);
		interface_s.setPreferredSize(new Dimension(100, 80));
		method_s = new JScrollPane(method_l);
		method_s.setPreferredSize(new Dimension(100, 100));

		// add the text fields
		provider_t = new JTextField(15);
		// interface_t = new JTextField(15);
		// method_t = new JTextField(15);

		// combine scroll panes and text fields into one pane
		provider_p = new JPanel();
		//interface_p = new JPanel();
		interface_p = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				interface_s, method_s);
		interface_s.setMinimumSize(new Dimension(100, 0));
		method_s.setMinimumSize(new Dimension(100, 150));
		interface_p.setResizeWeight(0.4);
		
		// method_p = new JPanel();
		provider_p.setLayout(new BorderLayout());
		// interface_p.setLayout(new BoxLayout(interface_p, BoxLayout.Y_AXIS));
		//interface_p.setLayout(new BorderLayout());
		provider_p.add(provider_s, BorderLayout.CENTER);
		provider_p.add(provider_t, BorderLayout.SOUTH);
		//interface_p.add(interface_s, BorderLayout.NORTH);
		//interface_p.add(method_s, BorderLayout.CENTER);
				
		// method_p.add(method_s, BorderLayout.CENTER);
		// method_p.add(method_t, BorderLayout.SOUTH);

		// add Title Border to the panes
		provider_p.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Provider"));
		interface_p.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Interface"));
		method_s.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Method"));

		// put everything into one panel
		// this.add(provider_p);
		// this.add(interface_p);
		// this.add(method_p);

		// this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setLayout(new BorderLayout());
		if (withProviderList) {
			JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					provider_p, interface_p);
			provider_p.setMinimumSize(new Dimension(100, 0));
			interface_p.setMinimumSize(new Dimension(100, 150));
			jsp.setResizeWeight(0.4);
			this.add(jsp, BorderLayout.CENTER);
		} else {
			interface_p.setMinimumSize(new Dimension(150, 300));
			this.add(interface_p, BorderLayout.CENTER);
		}

	}

	/**
	 * Contains the action listeners for the provider, interface, and method
	 * lists, as well as the search bars for each
	 * 
	 * 
	 * @param search
	 *            search string
	 * @param providerL
	 *            list selection listener for the provider list
	 * @param interfaceL
	 *            list selection listener for the interface List
	 * @param methodL
	 *            list selection listener for the method List
	 */
	public void addListeners(ActionListener search,
			ListSelectionListener providerL, ListSelectionListener interfaceL,
			ListSelectionListener methodL) {
		provider_l.addListSelectionListener(providerL);
		interface_l.addListSelectionListener(interfaceL);
		method_l.addListSelectionListener(methodL);

		provider_t.addActionListener(search);
		provider_t.setActionCommand(PROVIDER_SEARCH);
		// interface_t.addActionListener(search);
		// interface_t.setActionCommand(INTERFACE_SEARCH);
		// method_t.addActionListener(search);
		// method_t.setActionCommand(METHOD_SEARCH);
	}

	/**
	 * Updates the interface with the list of providers, interfaces, or methods
	 * 
	 * @param Model
	 *            BrowserModel
	 * @param updated_interface
	 *            interface to be updated
	 */
	public void update(Observable Model, Object updated_interface) {

		String list[];
		if (updated_interface.toString().equals(BrowserModel.PROVIDER_ADDED)) {
			BrowserModel model = (BrowserModel) Model;
			list = model.getProviders();
			for (int i = 0; i < list.length; i++)
				if (!listModel.contains(list[i]))
					listModel.addElement(list[i]);
		}

		if (updated_interface.toString().equals(BrowserModel.PROVIDER_UPDATED)) {
			BrowserModel model = (BrowserModel) Model;
			list = model.getProviders();
			listModel.clear();
			for (int i = 0; i < list.length; i++)
				listModel.addElement(list[i]);
			if (list.length == 1) // autoselect if only one item
				provider_l.setSelectedIndex(0);
		}
		if (updated_interface.toString().equals(BrowserModel.INTERFACE_UPDATED)) {
			BrowserModel model = (BrowserModel) Model;
			list = model.getInterfaces();
			interface_l.setListData(list);

			if (list.length == 1) // auto select if only one item
				interface_l.setSelectedIndex(0);
		}
		if (updated_interface.toString().equals(BrowserModel.METHOD_UPDATED)) {
			BrowserModel model = (BrowserModel) Model;
			list = model.getMethods();
			method_l.setListData(list);
			if (list.length == 1) // auto select if only one item
				method_l.setSelectedIndex(0);
		}

	}
	// done
}