/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.container.jeri;

import net.jini.core.constraint.MethodConstraints;
import net.jini.jeri.*;

import javax.inject.Provider;

/**
 * @author Rafał Krupiński
 */
public class ExporterFactory extends AbstractExporterFactory<BasicJeriExporter> {
    public ExporterFactory(InvocationLayerFactory ilFactory, boolean enableDGC, boolean keepAlive) {
        super(ilFactory, new TcpServerEndpointFactory(), enableDGC, keepAlive);
    }

    public ExporterFactory(InvocationLayerFactory ilFactory, Provider<? extends ServerEndpoint> serverEndpointProvider) {
        super(ilFactory, serverEndpointProvider);
    }

    public ExporterFactory(InvocationLayerFactory ilFactory) {
        this(ilFactory, new TcpServerEndpointFactory());
    }

    public ExporterFactory(Provider<? extends ServerEndpoint> serverEndpointProvider){
        this(new BasicILFactory(), serverEndpointProvider);
    }

    public ExporterFactory() {
        this(new BasicILFactory());
    }

    public static ExporterFactory trusted(MethodConstraints methodConstraints, Class permissionClass) {
        return new ExporterFactory(new ProxyTrustILFactory(methodConstraints, permissionClass));
    }

    @Override
    protected BasicJeriExporter doGet(ServerEndpoint serverEndpoint, InvocationLayerFactory ilFactory) {
        return new BasicJeriExporter(serverEndpoint, ilFactory, enableDGC, keepAlive);
    }
}
