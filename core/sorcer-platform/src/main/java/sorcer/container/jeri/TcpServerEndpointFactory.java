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

import net.jini.jeri.tcp.TcpServerEndpoint;
import sorcer.util.Sorcer;

import javax.inject.Provider;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.net.UnknownHostException;

/**
 * @author Rafał Krupiński
 */
public class TcpServerEndpointFactory implements Provider<TcpServerEndpoint> {
    private String host;
    private int port;
    private SocketFactory socketFactory;
    private ServerSocketFactory serverSocketFactory;

    /**
     * Construct TcpServerEndpointFactory with {@link SorcerEnv#getHostAddress()} as the listening address and port 0.
     *
     * @throws java.lang.IllegalStateException if the local address cannot be obtained.
     */
    public TcpServerEndpointFactory() {
        try {
            host = Sorcer.getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Could not resolve local address", e);
        }
    }

    public TcpServerEndpointFactory(int port) {
        this();
        this.port = port;
    }

    public TcpServerEndpointFactory(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public TcpServerEndpoint get() {
        return TcpServerEndpoint.getInstance(host, port, socketFactory, serverSocketFactory);
    }
}
