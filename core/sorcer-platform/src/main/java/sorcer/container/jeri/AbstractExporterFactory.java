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

import net.jini.export.Exporter;
import net.jini.jeri.InvocationLayerFactory;
import net.jini.jeri.ServerEndpoint;

import javax.inject.Provider;

/**
 * (Abstract)ExporterFactory implementations allow to configure multi-use factories for {@link Exporter}s sharing the same configuration.
 *
 * @author Rafał Krupiński
 */
public abstract class AbstractExporterFactory<T extends Exporter> {
    protected InvocationLayerFactory ilFactory;
    protected Provider<? extends ServerEndpoint> serverEndpointProvider;
    protected boolean enableDGC;
    protected boolean keepAlive;

    protected AbstractExporterFactory(InvocationLayerFactory ilFactory, Provider<? extends ServerEndpoint> serverEndpointProvider, boolean enableDGC, boolean keepAlive) {
        this.ilFactory = ilFactory;
        this.serverEndpointProvider = serverEndpointProvider;
        this.enableDGC = enableDGC;
        this.keepAlive = keepAlive;
    }

    public AbstractExporterFactory(InvocationLayerFactory ilFactory, Provider<? extends ServerEndpoint> serverEndpointProvider) {
        this(ilFactory, serverEndpointProvider, false, true);
    }

    public T get() {
        return doGet(serverEndpointProvider.get(), ilFactory);
    }

    public T get(InvocationLayerFactory ilFactory) {
        return doGet(serverEndpointProvider.get(), ilFactory);
    }

    protected abstract T doGet(ServerEndpoint serverEndpoint, InvocationLayerFactory ilFactory);
}
