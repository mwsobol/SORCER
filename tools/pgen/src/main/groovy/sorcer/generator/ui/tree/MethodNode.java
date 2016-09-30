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

import sorcer.Util;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Method;

/**
 * A node that represents a method
 *
 * @author Dennis Reedy
 */
public class MethodNode extends DefaultMutableTreeNode {
    private final Method method;
    private final String providerName;
    private final String providerInterface;

    public MethodNode(Method method, String providerName, String providerInterface) {
        this.method = method;
        this.providerName = providerName;
        this.providerInterface = providerInterface;
    }

    String getCompoundName() {
        return Util.getCompoundName(method, providerInterface);
    }

    public Method getMethod() {
        return method;
    }

    public String getProviderInterface() {
        return providerInterface;
    }

    public String getProviderName() {
        return providerName;
    }

    @Override public String toString() {
        return getCompoundName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MethodNode that = (MethodNode) o;
        return method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
