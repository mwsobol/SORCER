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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Displays options to be selected and filld in for project generation
 *
 * @author Dennis Reedy
 */
public class OptionsPanel extends JPanel {
    private final JCheckBox provider = new JCheckBox("Create a Provider");
    private final JCheckBox requestor = new JCheckBox("Create a Requestor");
    private final JTextField packageNameField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField versionField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final String group;
    private final String packageNamePattern;
    private File projectRootDir;

    public OptionsPanel(final String group, final String version, final String projectVersion, final String location) {
        super(new BorderLayout(8, 8));
        packageNameField.setText(group);
        versionField.setText(version);
        locationField.setText(location);
        projectRootDir = new File(location);
        this.group = group;

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));


        /* Only allow word characters: [a-zA-Z_0-9] */
        //((AbstractDocument)nameField.getDocument()).setDocumentFilter(new LowerCaseInputFilter("\\w"));
        ((AbstractDocument)nameField.getDocument()).setDocumentFilter(new InputFilter("[a-zA-Z_0-9\\.\\-\\_]", false));

        /* Only allow word characters: [a-zA-Z_0-9] and the '.' character*/
        //((AbstractDocument)packageNameField.getDocument()).setDocumentFilter(new LowerCaseInputFilter("[a-zA-Z_0-9\\.]"));
        //packageNamePattern = "[a-zA-Z_0-9\\.\\_]";
        packageNamePattern = "[a-zA-Z_0-9\\.]";
        ((AbstractDocument)packageNameField.getDocument()).setDocumentFilter(new InputFilter(packageNamePattern, true));

        JPanel whatToCreate = new JPanel();
        provider.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(((JCheckBox)e.getSource()).isSelected()) {
                    requestor.setEnabled(false);
                } else {
                    requestor.setEnabled(true);
                }
            }
        });
        requestor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((JCheckBox) e.getSource()).isSelected()) {
                    provider.setEnabled(false);
                } else {
                    provider.setEnabled(true);
                }
            }
        });
        //requestor.setEnabled(false);

        BoxLayout boxLayout = new BoxLayout(whatToCreate, BoxLayout.Y_AXIS);
        whatToCreate.setLayout(boxLayout);
        whatToCreate.add(provider);
        whatToCreate.add(Box.createVerticalStrut(4));
        whatToCreate.add(requestor);
        whatToCreate.add(Box.createVerticalStrut(4));
        whatToCreate.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Select the project type to create"),
                                                                  BorderFactory.createEmptyBorder(4, 4, 4,4)));

        ImageIcon mstcLogo = new ImageIcon(this.getClass().getResource("/images/mstc.png"));
        JPanel top = new JPanel();
        BoxLayout topLayout = new BoxLayout(top, BoxLayout.X_AXIS);
        top.setLayout(topLayout);
        top.add(whatToCreate);
        top.add(new JLabel(mstcLogo));

        JPanel coordinatesPane = new JPanel();
        GroupLayout groupLayout2 = new GroupLayout(coordinatesPane);
        coordinatesPane.setLayout(groupLayout2);
        coordinatesPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Provide project coordinates"),
                BorderFactory.createEmptyBorder(4, 4, 4,4)));

        JLabel locationLabel = new JLabel("Project root");
        locationLabel.setLabelFor(locationField);

        JLabel nameLabel = new JLabel("Name");
        nameLabel.setLabelFor(nameField);
        nameField.setToolTipText("The name of your project (also the directory name)");
        nameField.getDocument().addDocumentListener(new InputListener());
        JLabel versionLabel = new JLabel("Version");
        versionLabel.setLabelFor(versionField);
        versionField.setToolTipText("The version. You can declare specifically or inherit the project's version (currently "+projectVersion+")");

        JLabel packageNameLabel = new JLabel("Package");
        packageNameLabel.setLabelFor(packageNameField);
        packageNameField.setToolTipText("The Java package name");
        coordinatesPane.add(locationLabel);
        coordinatesPane.add(locationField);
        coordinatesPane.add(nameLabel);
        coordinatesPane.add(nameField);
        coordinatesPane.add(versionLabel);
        coordinatesPane.add(versionField);
        JButton choose = new JButton("Choose...");
        choose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setCurrentDirectory(projectRootDir);
                int returnVal = fileChooser.showOpenDialog(OptionsPanel.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    locationField.setText(fileChooser.getSelectedFile().getPath());
                }
            }
        });
        choose.setPreferredSize(new Dimension(12, 12));

        groupLayout2.setHorizontalGroup(groupLayout2.createSequentialGroup()
                                            .addGroup(groupLayout2.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                          .addComponent(nameLabel)
                                                          .addComponent(packageNameLabel)
                                                          .addComponent(versionLabel)
                                                          .addComponent(locationLabel))
                                            .addGroup(groupLayout2.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                          .addComponent(nameField)
                                                          .addComponent(packageNameField)
                                                          .addComponent(versionField)
                                                          .addComponent(locationField))
                                            .addComponent(choose));
        groupLayout2.setVerticalGroup(groupLayout2.createSequentialGroup()
                                          .addGroup(groupLayout2.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(nameLabel)
                                                        .addComponent(nameField))
                                          .addGroup(groupLayout2.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(packageNameLabel)
                                                        .addComponent(packageNameField))
                                          .addGroup(groupLayout2.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(versionLabel)
                                                        .addComponent(versionField))
                                          .addGroup(groupLayout2.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(locationLabel)
                                                        .addComponent(locationField)
                                                        .addComponent(choose)));

        add(top, BorderLayout.NORTH);
        add(coordinatesPane, BorderLayout.CENTER);
    }

    Options getOptions() {
        Options options = new Options();
        options.setProjectRootDir(projectRootDir);
        options.setLocation(new File(locationField.getText()));
        options.setName(nameField.getText());
        options.setVersion(versionField.getText());
        options.setProvider(provider.isSelected());
        options.setRequestor(requestor.isSelected());
        options.setGroup(group);
        options.setPackageName(packageNameField.getText());
        options.setClassName(nameField.getText().replaceAll("\\W", ""));
        return options;
    }

    boolean verify() {
        if(!provider.isSelected() && ! requestor.isSelected()) {
            JOptionPane.showMessageDialog(this,
                                          "You must select to create a Provider or a Requestor",
                                          "Project Creation Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(nameField.getText().length()==0) {
            JOptionPane.showMessageDialog(this,
                                          "You must provide a name",
                                          "Project Creation Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(versionField.getText().length()==0) {
            JOptionPane.showMessageDialog(this,
                                          "You must provide a version",
                                          "Project Creation Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(packageNameField.getText().length()==0) {
            JOptionPane.showMessageDialog(this,
                                          "You must provide a package name",
                                          "Project Creation Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(locationField.getText().length()==0) {
            JOptionPane.showMessageDialog(this,
                                          "You must provide a version",
                                          "Project Creation Error",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            String name = String.format("%s%s", nameField.getText(), requestor.isSelected()?"-req":"");
            File dir = new File(locationField.getText(), name);
            if(dir.exists()) {
                JOptionPane.showMessageDialog(this,
                                             "The project location ["+dir.getPath()+"]\nalready exists, provide a new location",
                                             "Project Creation Error",
                                             JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        return true;
    }

    /**
     * A filter that always inserts/replaces text as lower case and only allow words inserts/replacements
     * based on a regular expression
     */
    class InputFilter extends DocumentFilter {
        final String regEx;
        final boolean isLowerCase;

        public InputFilter(String regEx, boolean isLowerCase) {
            this.regEx = regEx;
            this.isLowerCase = isLowerCase;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String input, AttributeSet attr) throws BadLocationException {
            if(!allow(input))
                Toolkit.getDefaultToolkit().beep();
            else
                super.insertString(fb, offset, isLowerCase?input.toLowerCase():input, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String input, AttributeSet attrs) throws BadLocationException {
            if(!allow(input))
                Toolkit.getDefaultToolkit().beep();
            else
                super.replace(fb, offset, length, isLowerCase?input.toLowerCase():input, attrs);
        }

        boolean allow(String input) {
            //String input = text.equals("-")?"_":text;
            Matcher m = Pattern.compile(regEx).matcher(input);
            return m.find();
        }
    }

    class InputListener implements DocumentListener {

        @Override public void insertUpdate(DocumentEvent e) {
            getAndSetText(e);
        }

        @Override public void removeUpdate(DocumentEvent e) {
            getAndSetText(e);
        }

        @Override public void changedUpdate(DocumentEvent e) {
            getAndSetText(e);
        }

        void getAndSetText(DocumentEvent e) {
            Document document = e.getDocument();
            try {
                String s = document.getText(e.getOffset(), e.getLength()).trim();
                String input = s.equals("-")?"":s;
                Matcher m = Pattern.compile(packageNamePattern).matcher(input);
                if(m.find()|| input.length()==0) {
                    String text = document.getText(0, document.getLength()).replaceAll("-", "");
                    String value = text.length() > 0 ? String.format(".%s", text) : "";
                    packageNameField.setText(String.format("%s%s", group, value));
                }
            } catch (BadLocationException e1) {
                e1.printStackTrace();
                packageNameField.setText(e1.getClass().getName()+": "+e1.getMessage());
            }
        }
    }
}
