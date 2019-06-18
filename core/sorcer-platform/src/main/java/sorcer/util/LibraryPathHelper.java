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
package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Collections;

import static java.io.File.pathSeparator;
import static sorcer.util.JavaSystemProperties.LIBRARY_PATH;

/**
 * LibraryPathHelper implements a Set&lt;String> synchronized with system property java.library.requestReturn. each change is immediately reflected in the Java runtime. All Set operations are synchronized.
 *
 * @author Rafał Krupiński
 */
public class LibraryPathHelper extends AbstractSet<String> {
    private static final Logger log = LoggerFactory.getLogger(LibraryPathHelper.class);

    static Field fieldSysPath;

    static {
        fieldSysPath = prepareSysPathField();
    }

    private static Set<String> currentElements() {
        String[] elements = System.getProperty(LIBRARY_PATH, "").split(pathSeparator);
        Set<String> result = new LinkedHashSet<String>(elements.length);
        Collections.addAll(result, elements);
        return result;
    }

    private static Field prepareSysPathField() {
        try {
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            return fieldSysPath;
        } catch (Exception e) {
            throw new RuntimeException("Could not update java.library.requestReturn system property", e);
        }
    }

    public static Set<String> getLibraryPath() {
        return Collections.synchronizedSet(new LibraryPathHelper());
    }

    protected void updateLibraryPath(Set<String> newPaths) {
        Set<String> current = currentElements();
        if (current.equals(newPaths)) return;
        reportSetDifference(current, newPaths);

        System.setProperty(LIBRARY_PATH, StringUtils.join(newPaths, pathSeparator));

        try {
            fieldSysPath.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Could not update java.library.requestReturn system property", e);
        }
    }

    private void reportSetDifference(Collection<String> current, Collection<String> updated) {
        if (log.isInfoEnabled())
            log.info("Modifying {}: {} -> {}", LIBRARY_PATH, oneWayDiff(current, updated), oneWayDiff(updated, current));
    }

    private String oneWayDiff(Collection<String> old, Collection<String> updated) {
        List<String> result = new ArrayList<String>(old.size());
        for (String elem : old) {
            result.add(updated.contains(elem) ? elem : "[" + elem + "]");
        }
        return StringUtils.join(result, pathSeparator);
    }

    @Override
    public Iterator<String> iterator() {
        class OnlineIterator implements Iterator<String> {
            private Set<String> set;
            private Iterator<String> i;

            OnlineIterator(Set<String> set) {
                this.set = set;
                i = set.iterator();
            }

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public String next() {
                return i.next();
            }

            @Override
            public void remove() {
                i.remove();
                updateLibraryPath(set);
            }
        }
        return new OnlineIterator(currentElements());
    }

    @Override
    public int size() {
        return currentElements().size();
    }

    @Override
    public boolean add(String s) {
        Set<String> set = currentElements();
        boolean success = set.add(s);
        if (success) {
            updateLibraryPath(set);
        }
        return success;
    }

    @Override
    public boolean remove(Object o) {
        Set<String> set = currentElements();
        boolean success = set.remove(o);
        if (success) {
            updateLibraryPath(set);
        }
        return success;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Set<String> strings = currentElements();
        boolean set = strings.removeAll(c);
        updateLibraryPath(strings);
        return set;
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        Set<String> strings = currentElements();
        boolean set = strings.addAll(c);
        updateLibraryPath(strings);
        return set;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Set<String> set = currentElements();
        boolean result = set.retainAll(c);
        updateLibraryPath(set);
        return result;
    }

    public static String locateNativePath(String nativeFileName) {

        if (GenericUtil.isLinuxOrMac()) {
            String libPath = locateNativeLibPath(true, nativeFileName, "/");
            if (libPath==null)
                libPath = locateNativeLibPath(false, nativeFileName, "/usr");
            if (libPath==null)
                libPath = locateNativeLibPath(false, nativeFileName, "/root");
            if (libPath==null)
                libPath = locateNativeLibPath(false, nativeFileName, "/opt");
            if (libPath==null)
                libPath = locateNativeLibPath(false, nativeFileName, "/");
            return libPath;
        }
        return null;
    }

    public static String locateNativeLibPath(boolean useLocate, String nativeFileName, String startDirectory) {
        String nativeLibPath = null;
        try {
            List<String> results = new ArrayList<String>();
            List<String> errors = new ArrayList<String>();
            String[] cmds;
            if (useLocate)
                cmds = new String[] { "locate" , nativeFileName };
            else
                cmds = new String[] { "find" , "-key", nativeFileName };
            GenericUtil.execScript(cmds, new File(startDirectory), results, errors);
            for (String res : results) {
                if (res.endsWith(nativeFileName)) {
                    res = (res.substring(0, res.lastIndexOf(nativeFileName)));
                    if (res.startsWith(".")) res = res.substring(1);
                    return (useLocate ? "" : startDirectory) + res;
                }
            }
        } catch (IOException io) {
            log.debug("Could not locate native library requestReturn: " + io.getMessage());
        } catch (InterruptedException io) {
            log.debug("Could not locate native library requestReturn: " + io.getMessage());
        }
        return null;
    }
}
