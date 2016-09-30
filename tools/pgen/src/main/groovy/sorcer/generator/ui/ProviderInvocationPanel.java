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

import sorcer.Util;
import sorcer.generator.Options;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A panel that provides options for invoking providers from requestors
 *
 * @author Dennis Reedy
 */
public class ProviderInvocationPanel extends JPanel {
    private Options options;
    private JPanel invocationOptions;
    private final String SPACE = "space";
    private final String PROVISION = "provision";
    private Map<Method, Map<String, JCheckBox>> methodOptions = new HashMap<Method, Map<String, JCheckBox>>();

    public ProviderInvocationPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        JPanel top = new JPanel();
        BoxLayout boxLayout = new BoxLayout(top, BoxLayout.Y_AXIS);
        //top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        top.setLayout(boxLayout);
        top.add(new JLabel("Select invocation options for the provider methods you have chosen"));
        top.add(new JSeparator(SwingConstants.HORIZONTAL));
        add(top, BorderLayout.NORTH);
    }

    void setOptions(Options options) {
        if(invocationOptions!=null) {
            this.remove(invocationOptions);
        }
        methodOptions.clear();
        invocationOptions = new JPanel();
        BoxLayout boxLayout = new BoxLayout(invocationOptions, BoxLayout.Y_AXIS);
        invocationOptions.setLayout(boxLayout);
        this.options = options;
        for(Map.Entry<Method, Map<Options.ProviderData, String>> entry : options.getProviders().entrySet()) {
            JPanel methodOptionPanel = new JPanel();
            BoxLayout bl = new BoxLayout(methodOptionPanel, BoxLayout.Y_AXIS);
            methodOptionPanel.setLayout(bl);

            String name = Util.getCompoundName(entry.getKey(), entry.getValue().get(Options.ProviderData.INTERFACE));
            JLabel method = new JLabel(String.format("<html><b>%s</b></html>", name));
            methodOptionPanel.add(method);

            JCheckBox space = new JCheckBox("Use Space");
            space.setToolTipText("Use a pull strategy to handle requests, best for dynamic load distribution");
            JCheckBox provision = new JCheckBox("Use Dynamic Provisioning");
            provision.setToolTipText("Dynamically create the provider on available compute resources");
            Map<String, JCheckBox> optionMap = new HashMap<String, JCheckBox>();
            optionMap.put(SPACE, space);
            optionMap.put(PROVISION, provision);

            methodOptionPanel.add(space);
            methodOptionPanel.add(provision);

            invocationOptions.add(methodOptionPanel);
            invocationOptions.add(Box.createVerticalStrut(8));
            methodOptions.put(entry.getKey(), optionMap);
        }
        add(new JScrollPane(invocationOptions), BorderLayout.CENTER);
    }

    Options getOptions() {
        for(Map.Entry<Method, Map<String, JCheckBox>> entry : methodOptions.entrySet()) {
            Method method = entry.getKey();
            JCheckBox space = entry.getValue().get(SPACE);
            JCheckBox provision = entry.getValue().get(PROVISION);
            Map<Options.ProviderData, String> metaData = options.getProviders().get(method);
            metaData.put(Options.ProviderData.USE_SPACE, Boolean.toString(space.isSelected()));
            metaData.put(Options.ProviderData.DEPLOY, Boolean.toString(provision.isSelected()));
            options.getProviders().put(method, metaData);
        }
        return options;
    }
}
