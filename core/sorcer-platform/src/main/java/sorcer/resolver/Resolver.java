/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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
package sorcer.resolver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Stub - added for compatibility with Sorcersoft.com SORCER - it is a helper class resolving ArtifactCoordinates to relative or absolute paths
 * original @author Rafał Krupiński
 */
public class Resolver {

	/**
	 * Resolve artifact coordinates to absolute path
	 *
	 * @return absolute path of file denoted bu the artifact coordinates
	 */
	public static String resolveAbsolute(String coords) {
		return null;
	}

	public static File resolveAbsoluteFile(String coords) {
		return null;
	}

	/**
	 * Resolve artifact coordinates to absolute path
	 *
	 * @return absolute path of file denoted bu the artifact coordinates
	 */

	public static String resolveRelative(String coords){
		return null;
	}

    public static String resolveAbsolute(URL baseUrl, String coords) {
        return null;
    }

	/**
	 * This is helper method for use in *.config files. The resulting string is
	 * passed to SorcerServiceDescriptor constructor as a codebase string.
	 *
	 * @param baseUrl URL root of artifacts
	 * @param coords  array of artifact coordinates
	 */
	public static String resolveCodeBase(URL baseUrl, String... coords) throws MalformedURLException {
		return null;
	}

	public static String resolveClassPath(String[] artifactCoordinatesList) {
		return null;
	}

	/**
	 * resolve jar by simple name (with extension). Maven version will use the name as artifactId and guess groupId and version. Flattened version will search for that file in all its roots.
	 * <p/>
	 * This is intended as a helper for MANIFEST Main-Class entries
	 */
	public static File resolveSimpleName(String simpleName) {
	    return null;
	}
}
