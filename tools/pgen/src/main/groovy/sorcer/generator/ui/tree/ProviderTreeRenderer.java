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
package sorcer.generator.ui.tree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom tree renderer for a provider tree
 * @author Dennis Reedy
 */
public class ProviderTreeRenderer extends DefaultTreeCellRenderer {
    private final ImageIcon providerIcon;
    private final ImageIcon classIcon;
    private final ImageIcon methodIcon;

    public ProviderTreeRenderer(ImageIcon providerIcon, ImageIcon classIcon, ImageIcon methodIcon) {
        this.providerIcon = providerIcon;
        this.classIcon = classIcon;
        this.methodIcon = methodIcon;
    }

    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean isSelected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

        super.getTreeCellRendererComponent(tree,
                                           value,
                                           isSelected,
                                           expanded,
                                           leaf,
                                           row,
                                           hasFocus);
        if(value instanceof ProviderNode) {
            setIcon(providerIcon);
        } else if(value instanceof ClassNode) {
            setIcon(classIcon);
            setToolTipText(((ClassNode)value).getName());
        } else if(value instanceof MethodNode) {
            setIcon(methodIcon);
            setToolTipText(((MethodNode)value).getCompoundName());
            this.setText(((MethodNode)value).getMethod().getName());
        } else if(value instanceof VersionNode) {
            VersionNode vNode = (VersionNode)value;
            if(vNode.hasError()) {
                setText("<html><strike>" + getText() + "</strike></html>");
            }
            setIcon(null);
            setToolTipText(null);
        } else if(value instanceof ExceptionNode) {
            setText("<html><font color=red>" + getText() + "</font></html>");
            setIcon(null);
            setToolTipText(null);
        } else {
            setIcon(null);
            setToolTipText(null);
        }

        return this;
    }
}
