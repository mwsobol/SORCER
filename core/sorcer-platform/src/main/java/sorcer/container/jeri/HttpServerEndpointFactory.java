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

import net.jini.jeri.http.HttpServerEndpoint;
import sorcer.util.Sorcer;

import javax.inject.Provider;
import java.net.UnknownHostException;

/**
 * @author Paweł Rubach
 */
public class HttpServerEndpointFactory implements Provider<HttpServerEndpoint> {
    private String host;

    /**
     * Construct TcpServerEndpointFactory with {@link sorcer.core.SorcerEnv#getHostAddress()} as the listening address and port 0.
     *
     * @throws IllegalStateException if the local address cannot be obtained.
     */
    public HttpServerEndpointFactory() {
        try {
            host = Sorcer.getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Could not resolve local address", e);
        }
    }

    @Override
    public HttpServerEndpoint get() {
        return HttpServerEndpoint.getInstance(host, 0);
    }
}
