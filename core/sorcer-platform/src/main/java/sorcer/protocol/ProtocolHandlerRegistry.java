package sorcer.protocol;
/**
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafał Krupiński
 */
public class ProtocolHandlerRegistry implements URLStreamHandlerFactory {
    private Map<String, URLStreamHandler> handlers = new HashMap<String, URLStreamHandler>();

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return handlers.get(protocol);
    }

    public void register(String protocol, URLStreamHandler handler) {
        handlers.put(protocol, handler);
    }

    private static ProtocolHandlerRegistry instance;
    private static boolean installed;

    public static synchronized ProtocolHandlerRegistry get() {
        if (!installed)
            try {
                ProtocolHandlerRegistry registry = new ProtocolHandlerRegistry();
                URL.setURLStreamHandlerFactory(registry);
                instance = registry;
                installed = true;
            } catch (Error e) {
                throw new IllegalStateException("Could not install URLStreamHandlerFactory", e);
            }
        return instance;
    }
}
