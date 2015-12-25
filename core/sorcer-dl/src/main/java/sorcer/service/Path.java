/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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

package sorcer.service;

import java.util.ArrayList;
import java.util.List;

public class Path implements Arg {

	private static final long serialVersionUID = 1L;

	public String path = null;

	public Object info = null;

	public Path() {
	}

	public Path(String path) {
		this.path = path;
	}

	public Path(String path, Object info) {
		this.path = path;
		this.info = info;
	}

	public String path() {
		return this.path;
	}

	public Object info() {
		return this.info;
	}

	@Override
	public String getName() {
		return path;
	}

	public static Path[] getSigPathArray(List<Path> paths) {
		Path[] sigPaths = new Path[paths.size()];
		return paths.toArray(sigPaths);
	}

	public static List<String> getPathList(Path[] sigPaths) {
		List<String> paths = new ArrayList(sigPaths.length);
		for (int i = 0; i < sigPaths.length; i++)
			paths.add(sigPaths[i].path);

		return paths;
	}

	public static List<Path> getPathList(List<String> paths) {
		ArrayList sigPaths = new ArrayList(paths.size());

		for(int i = 0; i < paths.size(); ++i) {
			sigPaths.add(new Path(paths.get(i)));
		}

		return sigPaths;
	}

	public static String[] getPaths(Path[] sigPaths) {
		String[] paths = new String[sigPaths.length];
		for (int i = 0; i < sigPaths.length; i++)
			paths[i] = sigPaths[i].path;

		return paths;
	}

	public static String[] getPaths(List<Path> sigPaths) {
		String[] paths = new String[sigPaths.size()];
		for (int i = 0; i < sigPaths.size(); i++)
			paths[i] = sigPaths.get(i).path;

		return paths;
	}

	public static Path[] getSigPaths(List<String> paths) {
		Path[] sigPaths = new Path[paths.size()];
		for (int i = 0; i < paths.size(); i++)
			sigPaths[i] = new Path(paths.get(i));

		return sigPaths;
	}
}