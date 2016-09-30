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
import sorcer.generator.ProviderGatherer;
import sorcer.generator.ProviderModule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * A frame for the project generator
 */
public class ProjectFrame extends JFrame implements ActionListener, GenerationListener {
    private final JButton previous = new JButton("Previous");
    private final JButton next = new JButton("Next");
    private final JButton cancel = new JButton("Cancel");
    private final JPanel cards;
    private final OptionsPanel optionsPanel;
    private final RequestorPanel requestorPanel = new RequestorPanel();
    private final ProviderInvocationPanel providerInvocationPanel = new ProviderInvocationPanel();
    private final ConfirmPanel confirmPanel = new ConfirmPanel();
    private final GenerationPanel generationPanel = new GenerationPanel();

    public ProjectFrame(final String group,
                        final String version,
                        final String projectVersion,
                        final String location,
                        Collection<ProviderModule> providers) throws HeadlessException {
        super("SORCER Project Generator");

        JPanel base = new JPanel(new BorderLayout(8, 8));

        optionsPanel = new OptionsPanel(group, version, projectVersion, location);
        optionsPanel.setName("options");
        requestorPanel.setName("requestor");
        requestorPanel.setProjectProviders(providers);
        providerInvocationPanel.setName("providerInvoke");
        confirmPanel.setName("confirm");
        generationPanel.setName("generate");

        JPanel buttonPane = new JPanel();
        previous.addActionListener(this);
        next.addActionListener(this);
        cancel.addActionListener(this);
        previous.setEnabled(false);
        buttonPane.add(previous);
        buttonPane.add(next);
        buttonPane.add(cancel);

        CardLayout cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.add(optionsPanel, "options");
        cards.add(requestorPanel, "requestor");
        cards.add(providerInvocationPanel, "providerInvoke");
        cards.add(confirmPanel, "confirm");
        cards.add(generationPanel, "generate");
        cardLayout.show(cards, "options");

        base.add(cards, BorderLayout.CENTER);
        base.add(buttonPane, BorderLayout.SOUTH);
        getContentPane().add(base);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println(getSize());
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
    }


    @Override
    public void setVisible(final boolean show) {
        if(show) {
            int width = 730;
            int height = 380;
            setSize(new Dimension(width, height));
            setLocationRelativeTo(null);toFront();
            super.setVisible(show);
            setAlwaysOnTop(true);
            requestFocus();
        } else {
            super.setVisible(show);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CardLayout cardLayout = (CardLayout) cards.getLayout();
        String currentCard = null;
        for (Component comp : cards.getComponents()) {
            if (comp.isVisible()) {
                currentCard = comp.getName();
                break;
            }
        }
        if (e.getSource() == next){
            previous.setEnabled(true);
            if(currentCard!=null && currentCard.equals("confirm")) {
                cardLayout.show(cards, "generate");
                Options options = confirmPanel.getOptions();
                generationPanel.generate(options, this);
                previous.setEnabled(false);
                next.setEnabled(false);
                cancel.setEnabled(false);
                cancel.setText("Complete");
            } else if(currentCard!=null && currentCard.equals("requestor")) {
                if(requestorPanel.verify()) {
                    providerInvocationPanel.setOptions(requestorPanel.getOptions());
                    cardLayout.show(cards, "providerInvoke");
                    confirmPanel.applyOptions(requestorPanel.getOptions());
                }
            } else if(currentCard!=null && currentCard.equals("providerInvoke")) {
                cardLayout.show(cards, "confirm");
                confirmPanel.applyOptions(providerInvocationPanel.getOptions());
            } else {
                if(optionsPanel.verify()) {
                    Options options = optionsPanel.getOptions();
                    if(options.isRequestor()) {
                        requestorPanel.setOptions(options);
                        cardLayout.show(cards, "requestor");
                    } else {
                        cardLayout.show(cards, "confirm");
                        confirmPanel.applyOptions(options);
                    }
                }
            }
        } else if(e.getSource()==previous) {
            if(currentCard!=null && currentCard.equals("confirm") && optionsPanel.getOptions().isRequestor()) {
                cardLayout.show(cards, "providerInvoke");
            } else if(currentCard!=null && currentCard.equals("providerInvoke") && optionsPanel.getOptions().isRequestor()) {
                cardLayout.show(cards, "requestor");
            } else {
                previous.setEnabled(false);
                next.setEnabled(true);
                cardLayout.show(cards, "options");
            }
        } else {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    static File getDir(File base, String root) {
        File file = null;

        for(File f : base.listFiles()) {
            if(f.getName().startsWith(root)) {
                file = f;
                break;
            }
        }
        return file;
    }

    public static void main(String[] args) throws IOException {
        String cwd = System.getProperty("user.dir");
        File distDir = new File(cwd, "../distributions/sorcer-modeling-2.5.16-develop");
        File sorcerLib = new File(distDir, "lib/sorcer/lib");
        File sorcerDlLib = new File(distDir, "lib/sorcer/lib-dl");
        File sorcerHome = getDir(distDir, "rio");
        File rioLib = new File(sorcerHome, "lib");
        File rioDlLib = new File(sorcerHome, "lib-dl");
        Map<String, String> versions = new HashMap();
        versions.put("engOpenVersion", "3.0-develop");
        versions.put("engGovVersion", "3.0-develop");
        java.util.List<ProviderModule> providers =
            ProviderGatherer.gatherFromLocalRepo("org.sorcer",
                                                 new File(System.getProperty("user.home"), ".m2/repository"),
                                                 versions,
                                                 sorcerLib, sorcerDlLib, rioLib, rioDlLib);
        new ProjectFrame("org.sorcer",
                         "${engOpenVersion}",
                         "2.1",
                         System.getProperty("user.dir"),
                         providers).setVisible(true);
    }

    @Override public void notifyComplete() {
        cancel.setEnabled(true);
    }
}
