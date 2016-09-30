/*
 * Copyright to the original author or authors.
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
package sorcer.generator.ui;

import sorcer.generator.Options;
import sorcer.generator.ProviderInfo;
import sorcer.generator.ProviderModule;
import sorcer.generator.ui.tree.MethodNode;
import sorcer.generator.ui.tree.ProviderTree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides additional setup for creating a requestor project
 *
 * @author Dennis Reedy
 */
public class RequestorPanel extends JPanel {
    private final ProviderTree providerTree;
    private final JList<MethodNode> selectedProviders;
    private final DefaultListModel<MethodNode> selectedProvidersListModel = new DefaultListModel<MethodNode>();
    private Options options;

    public RequestorPanel() {
        super(new BorderLayout(8, 8));
        //projectProviders = new JList<String>(projectProvidersListModel);
        //projectProviders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedProviders = new JList<MethodNode>(selectedProvidersListModel);

        JPanel base = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        base.setLayout(gridBag);
        base.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel buttons = new JPanel();
        BoxLayout boxLayout1 = new BoxLayout(buttons, BoxLayout.Y_AXIS);
        buttons.setLayout(boxLayout1);
        JButton add = new JButton("Add >>");
        add.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                MethodNode methodNode = providerTree.getSelectedItem();
                if(methodNode!=null) {
                    if(!selectedProvidersListModel.contains(methodNode))
                        selectedProvidersListModel.addElement(methodNode);
                }
            }
        });
        JButton remove = new JButton("Remove");
        remove.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int[] selected = selectedProviders.getSelectedIndices();
                /* Reverse the order using a bubble sort */
                boolean sorting = true;
                while(sorting) {
                    sorting = false;
                    for (int i = 0; i < selected.length-1; i++) {
                        if(selected[i] < selected[i+1]) {
                            int temp = selected[i];
                            selected[i] = selected[i+1];
                            selected[i+1] = temp;
                            sorting = true;
                        }
                    }
                }
                for(int index : selected) {
                    selectedProvidersListModel.removeElementAt(index);
                }
            }
        });
        buttons.add(add);
        buttons.add(remove);

        //JScrollPane projProvScroller = new JScrollPane(projectProviders);
        providerTree = new ProviderTree();
        JScrollPane selectedProvScroller = new JScrollPane(selectedProviders);

        GridBagConstraints c = new GridBagConstraints();

        /* Add project providers */
        c.gridwidth = 3;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        JPanel p1 = createInnerListPanel("Available Providers:", providerTree);
        gridBag.setConstraints(p1, c);
        base.add(p1);

        /* Reset grid bag constraints, add buttons */
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridBag.setConstraints(buttons, c);
        base.add(buttons);

        /* Reset grid bag constraints, add selected providers */
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        JPanel p2 = createInnerListPanel("Selected Providers:", selectedProvScroller);
        gridBag.setConstraints(p2, c);
        base.add(p2);

        JPanel top = new JPanel();
        BoxLayout boxLayout2 = new BoxLayout(top, BoxLayout.Y_AXIS);
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        top.setLayout(boxLayout2);
        top.add(new JLabel("Select the providers you want to use"));
        top.add(new JSeparator(SwingConstants.HORIZONTAL));
        add(top, BorderLayout.NORTH);
        add(base, BorderLayout.CENTER);
    }

    void setOptions(Options options) {
        this.options = options;
    }

    Options getOptions() {
        options.getProviders().clear();
        Enumeration<MethodNode> enumeration = selectedProvidersListModel.elements();
        while(enumeration.hasMoreElements()) {
            MethodNode methodNode = enumeration.nextElement();
            Map<Options.ProviderData, String> metaData = new HashMap<Options.ProviderData, String>();
            metaData.put(Options.ProviderData.NAME, methodNode.getProviderName());
            metaData.put(Options.ProviderData.INTERFACE, methodNode.getProviderInterface());
            /* get the simple classname */
            int ndx = methodNode.getProviderInterface().lastIndexOf('.');
            String simpleName = methodNode.getProviderInterface().substring(ndx+1);
            metaData.put(Options.ProviderData.SIMPLE_NAME, simpleName);
            options.getProviders().put(methodNode.getMethod(),metaData);
        }
        return options;
    }


    boolean verify() {
        if(selectedProvidersListModel.size()==0) {
            JOptionPane.showMessageDialog(this,
                                          "You must select project providers to use",
                                          "Project Creation Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private JPanel createInnerListPanel(String label, Component comp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    public void setProjectProviders(final Collection<ProviderModule> providers) {
        providerTree.setProjectProviders(providers);
    }
}
