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
import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Lists options to be confirmed before generating
 *
 * @author Dennis Reedy
 */
public class ConfirmPanel extends JPanel {
    private Options options;
    private final JLabel provider = new JLabel();
    private final JLabel providerInterface = new JLabel();
    private final JLabel providerImpl = new JLabel();
    private final JLabel requestor = new JLabel();
    private final JLabel providers = new JLabel();
    private final JLabel version = new JLabel();
    private final JLabel packageName = new JLabel();
    private final JLabel location = new JLabel();
    private final JLabel[] labels = {provider, providerInterface, providerImpl, requestor, version, packageName, location, providers};

    public ConfirmPanel() {
        super();
        BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        super.setLayout(layout);
        for (int i=0; i < labels.length; i++) {
            if(i>0)
                add(Box.createVerticalStrut(8));
            add(labels[i]);
        }
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    void applyOptions(final Options options) {
        this.options = options;
        for(JLabel label : labels) {
            label.setText("");
        }
        if(options.isProvider()) {
            provider.setText(String.format("<html><b>Provider to create: </b>%s</html>",
                                           options.getName()));
            providerInterface.setText(String.format("<html><b>Provider interface class: </b>%s.%s</html>",
                                               options.getPackageName(), options.getCapName()));
            providerImpl.setText(String.format("<html><b>Provider implementation class: </b>%s.provider.%sProviderImpl</html>",
                                               options.getPackageName(), options.getCapName()));
            location.setText(String.format("<html><b>Location: </b>%s%s%s</html>",
                                           options.getLocation().getPath(),
                                           File.separator,
                                           options.getName()));
        }
        if(options.isRequestor()) {
            requestor.setText(String.format("<html><b>Requestor to create: </b>%s</html>",
                                            options.getName()));
            StringBuilder pList = new StringBuilder();
            for(Map.Entry<Method, Map<Options.ProviderData, String>> entry : options.getProviders().entrySet()) {
                if(pList.length()>0)
                    pList.append(", ");
                pList.append(Util.getCompoundName(entry.getKey(), entry.getValue().get(Options.ProviderData.INTERFACE)));
            }
            providers.setText(String.format("<html><b>Providers to use:</b> %s</html>",
                                            pList.toString()));
            location.setText(String.format("<html><b>Location: </b>%s%s%s-req</html>",
                                           options.getLocation().getPath(),
                                           File.separator,
                                           options.getName()));
        }
        version.setText(String.format("<html><b>Version: </b>%s</html>", options.getVersion()));
        packageName.setText(String.format("<html><b>Package: </b>%s</html>", options.getPackageName()));

    }

    Options getOptions() {
        return options;
    }
}
