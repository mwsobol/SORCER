/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
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

import sorcer.service.modeling.Functionality;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Paths extends ArrayList<Path> implements Arg {
    private static final long serialVersionUID = 1L;

    public String name;

    public Functionality.Type type;

    public Paths() {
        super();
    }

    public Paths(int capacity) {
        super(capacity);
    }

    public Paths(Path[] paths) {
        for (Path path : paths) {
            add(path) ;
        }
    }
    public Paths(String[] names) {
        for (String name : names) {
            add(new Path(name)) ;
        }
    }

    public List<String> toStringList() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            paths.add(get(i).path);
        }

        return paths;
    }

    public String[] toStringArray() {
        String[] paths = new String[size()];
        for (int i = 0; i < size(); i++) {
            paths[i] = get(i).path;
        }

        return paths;
    }

    public Path[] toPathArray() {
        Path[] paths = new Path[size()];
        return this.toArray(paths);
    }

    public Path getPath(String path) {
        for (Path p : this) {
            if (p.path.equals(path)) {
                return p;
            }
        }
        return null;
    }

    public boolean containsPath(String path) {
        for (Path p : this) {
            if (p.path.equals(path)) {
                return true;
            }
        }
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object execute(Arg... args) throws ServiceException, RemoteException {
        return this;
    }
}

