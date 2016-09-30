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

import sorcer.generator.ProviderInfo;
import sorcer.generator.ProviderModule;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A tree for showing providers in a JTree
 * @author Dennis Reedy
 */
public class ProviderTree extends JPanel implements TreeExpansionListener {
    private final JTree tree;
    private final DefaultMutableTreeNode root;
    private final DefaultTreeModel treeModel;

    public ProviderTree() {
        super(new BorderLayout(8, 8));
        root = new DefaultMutableTreeNode("project-providers");
        treeModel = new DefaultTreeModel(root);

        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(new VetoableTreeSelectionModel());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeExpansionListener(this);
        ImageIcon providerNodeIcon = new ImageIcon(this.getClass().getResource("/images/providerNode.png"));
        ImageIcon classNodeIcon = new ImageIcon(this.getClass().getResource("/images/classNode.png"));
        ImageIcon methodNodeIcon = new ImageIcon(this.getClass().getResource("/images/methodNode.png"));
        tree.setCellRenderer(new ProviderTreeRenderer(providerNodeIcon, classNodeIcon, methodNodeIcon));
        javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);
        add(new JScrollPane(tree));
    }

    public void setProjectProviders(Collection<ProviderModule> providers) {
        for(ProviderModule provider : providers) {
            DefaultMutableTreeNode pNode = new ProviderNode(provider);
            root.add(pNode);
        }
        tree.expandPath(new TreePath(root.getPath()));
    }

    public MethodNode getSelectedItem() {
        if(tree.getSelectionCount()==0)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        return (MethodNode)node;
    }

    @Override public void treeExpanded(final TreeExpansionEvent event) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
                if(node instanceof ProviderNode && node.getChildCount()==0) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        ProviderModule providerModule = ((ProviderNode)node).getProviderModule();
                        if(providerModule.getProviderInfos().isEmpty()) {
                            Toolkit.getDefaultToolkit().beep();
                            throw new RuntimeException("NO ProviderInfo!");
                        }
                        for(ProviderInfo info : providerModule.getProviderInfos()) {
                            VersionNode versionNode = new VersionNode(info.getVersion());
                            treeModel.insertNodeInto(versionNode, node, node.getChildCount());
                            try {
                                Collection<ClassNode> classNodes = getClassNodes(info);
                                if(classNodes.isEmpty()) {
                                    versionNode.setHasError(true);
                                    treeModel.insertNodeInto(new ExceptionNode("Found no matching interfaces for "+
                                                                               info.getBaseName()),
                                                             versionNode, versionNode.getChildCount());
                                    continue;
                                }
                                for (ClassNode classNode : classNodes) {
                                    Class<?> klass = classNode.loadClass();
                                    if (klass.isInterface() && Remote.class.isAssignableFrom(klass)) {
                                        Method[] methods = klass.getMethods();
                                        if (methods.length > 0) {
                                            treeModel.insertNodeInto(classNode, versionNode, versionNode.getChildCount());
                                            for (Method m : methods) {
                                                treeModel.insertNodeInto(new MethodNode(m,
                                                                                        info.getBaseName(),
                                                                                        classNode.getName()),
                                                                         classNode,
                                                                         classNode.getChildCount());
                                            }
                                        }
                                    } /*else {
                                        versionNode.setHasError(true);
                                        treeModel.insertNodeInto(new ExceptionNode(klass.getName()+" is not assignable from "+
                                                                                   Remote.class.getName()),
                                                                 versionNode, versionNode.getChildCount());
                                    }*/

                                }

                            } catch (Throwable t) {
                                versionNode.setHasError(true);
                                treeModel.insertNodeInto(new ExceptionNode(t),
                                                         versionNode, versionNode.getChildCount());
                                t.printStackTrace();
                            }
                        }
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    tree.expandPath(new TreePath(node.getPath()));
                }

            }
        });
    }

    @Override public void treeCollapsed(TreeExpansionEvent event) {
    }

    private Collection<ClassNode> getClassNodes(ProviderInfo info) throws IOException {
        java.util.List<ClassNode> classNodes = new ArrayList<ClassNode>();
        JarFile jarFile = new JarFile(info.getApiJar());
        Enumeration<JarEntry> entries = jarFile.entries();
        while(entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if(entry.isDirectory())
                continue;
            if(entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                String dotName = entry.getName().replaceAll("/", ".");
                String name = dotName.substring(0, dotName.length() - ".class".length());
                int ndx = name.lastIndexOf('.');
                String simpleName = name.substring(ndx+1);
                if(simpleName.toLowerCase().equals(info.getBaseName().toLowerCase()) ||
                   simpleName.toLowerCase().endsWith("interface")) {
                    classNodes.add(new ClassNode(name, simpleName, info.getClassLoader()));
                } else {
                    System.out.println("UH OH -> "+simpleName.toLowerCase()+" does not match "+info.getBaseName().toLowerCase());
                }
            }
        }
        return classNodes;
    }
}
