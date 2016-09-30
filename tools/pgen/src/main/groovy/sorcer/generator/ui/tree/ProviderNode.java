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

import sorcer.generator.ProviderModule;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * A tree node that contains a provider
 *
 * @author Dennis Reedy
 */
public class ProviderNode extends DefaultMutableTreeNode {
    private ProviderModule providerModule;

    public ProviderNode(final ProviderModule providerModule) {
        this.providerModule = providerModule;
    }

    @Override public boolean isLeaf() {
        return false;
    }

    @Override public String toString() {
        return providerModule.getBaseName();
    }

    @Override public boolean getAllowsChildren() {
        return true;
    }

    public ProviderModule getProviderModule() {
        return providerModule;
    }
}
