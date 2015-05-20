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

import net.jini.jeri.BasicJeriExporter;

/**
 * @author Rafał Krupiński
 */
public class ExporterFactories {
    /**
     * ExporterFactory with BasicILFactory and TcpServerEndpoint
     */
    public static final ExporterFactory EXPORTER = new ExporterFactory();

    /**
     * ExporterFactory with ProxyTrustILFactory and TcpServerEndpoint
     */
    public static final ExporterFactory TRUSTED = ExporterFactory.trusted(null, null);

    /**
     * ExporterFactory with BasicILFactory and HttpServerEndpoint
     */
    public static final HttpExporterFactory HTTP = new HttpExporterFactory();

    /**
     * @return BasicJeriExporter created by {@link #EXPORTER} {@link ExporterFactory}
     */
    public static BasicJeriExporter getBasicTcp() {
        return EXPORTER.get();
    }

    /**
     * @return BasicJeriExporter created with {@link #TRUSTED} {@link ExporterFactory}
     */
    public static BasicJeriExporter getTrustedTcp() {
        return TRUSTED.get();
    }

    /**
     * @return BasicJeriExporter created with {@link #HTTP} {@link HttpExporterFactory}
     */
    public static BasicJeriExporter getHttp() {
        return HTTP.get();
    }
}
