/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

package sorcer.util.url.sos;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Mike Sobolewski
 */
public class Handler extends URLStreamHandler {

	public static void register() {
		final String packageName = Handler.class.getPackage().getName();
		final String pkg = packageName.substring(0,
				packageName.lastIndexOf('.'));
		final String protocolPathProp = "java.protocol.handler.pkgs";

		String uriHandlers = System.getProperty(protocolPathProp, "");
		if (uriHandlers.indexOf(pkg) == -1) {
			if (uriHandlers.length() != 0)
				uriHandlers += "|";
			uriHandlers += pkg;
			System.setProperty(protocolPathProp, uriHandlers);
		}
	}
    
	/* (non-Javadoc)
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
	@Override
	protected URLConnection openConnection(URL url) throws IOException {
	        return new SdbConnection(url);
	}

}
