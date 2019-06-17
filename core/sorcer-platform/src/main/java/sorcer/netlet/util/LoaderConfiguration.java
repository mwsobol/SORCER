/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.netlet.util;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to configure a RootLoader from a stream or by using
 * it's methods.
 * <p/>
 * The stream can be for example a FileInputStream from a file with
 * the following format:
 * <p/>
 * <pre>
 * # comment
 * main is classname
 * load path
 * load file
 * load pathWith${property}
 * load pathWith!{required.property}
 * load path/*.jar
 * load path/&#42;&#42;/&#42;.jar
 * </pre>
 * <ul>
 * <li>All lines starting with "#" are ignored.</li>
 * <li>The "main is" part may only be once in the file. The String
 * afterwards is the key of a class with a main method. </li>
 * <li>The "load" command will add the given file or path to the
 * classpath in this configuration object. If the path does not
 * exist, the path will be ignored.
 * </li>
 * <li>properties referenced using !{x} are required.</li>
 * <li>properties referenced using ${x} are not required. If the
 * property does not exist the whole load instruction line will
 * be ignored.</li>
 * <li>* is used to match zero or more characters in a file.</li>
 * <li>** is used to match zero or more directories.</li>
 * <li>Loading paths with <code>load ./*.jar</code> or <code>load *.jar</code> are not supported.</li>
 * </ul>
 * <p/>
 * Defining the main class is required unless setRequireMain(boolean) is
 * called with false, before reading the configuration.
 * You can use the wildcard "*" to filter the path, but only for files, not
 * directories. To match directories use "**". The ${propertyname} is replaced by the eval of the system's
 * property key. You can use user.home here for example. If the property does
 * not exist, an empty string will be used. If the path or file after the load
 * command does not exist, the path will be ignored.
 *
 * @author Jochen Theodorou
 * @version $Revision$
 */
public class LoaderConfiguration {

    private static final String MAIN_PREFIX = "main is", PROP_PREFIX = "property";
    private List classPath = new ArrayList();
    private String main;
    private boolean requireMain;
    static final Logger logger = LoggerFactory.getLogger(LoaderConfiguration.class.getName());

    /**
     * creates a new loader configuration
     */
    public LoaderConfiguration() {
        this.requireMain = false;
    }

    /**
     * configures this loader with a stream
     *
     * @param is stream used to read the configuration
     * @throws java.io.IOException if reading or parsing the contents of the stream fails
     */
    public void configure(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int lineNumber = 0;

        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            line = line.trim();
            lineNumber++;

            if (line.startsWith("#") || line.length() == 0) continue;

            if (line.startsWith(LoaderConfigurationHelper.LOAD_PREFIX)) {
                String loadPath = line.substring(LoaderConfigurationHelper.LOAD_PREFIX.length()).trim();
                loadUrls(LoaderConfigurationHelper.load(loadPath));
            } else if (line.startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX)) {
                // Webster is not yet started so this will work only if we use jars from external websters
                // specified using: http://.... or mvn://... @address:port
                // or if the environment points to a running system webster
                List<String> codebasePaths = new ArrayList<String>();
                codebasePaths.add(line);
                loadUrls(LoaderConfigurationHelper.setCodebase(codebasePaths, System.out));
            } else if (line.startsWith(MAIN_PREFIX)) {
                if (main != null)
                    throw new IOException("duplicate definition of main in line " + lineNumber + " : " + line);
                main = line.substring(MAIN_PREFIX.length()).trim();
            } else if (line.startsWith(PROP_PREFIX)) {
                String params = line.substring(PROP_PREFIX.length()).trim();
                int index = params.indexOf('=');
                if (index == -1) {
                    throw new IOException("unexpected property format - expecting key=eval" + lineNumber + " : " + line);
                }
                String propName = params.substring(0, index);
                String propValue= LoaderConfigurationHelper.assignProperties(params.substring(index + 1));
                System.setProperty(propName, propValue);
            } else {
                throw new IOException("unexpected line in " + lineNumber + " : " + line);
            }
        }

        if (requireMain && main == null) throw new IOException("missing main class definition in config file");
    }

    /**
     * Load a possibly filtered path. Filters are defined
     * by using the * wildcard like in any shell.
     */
    private void loadUrls(List<URL> urls) {
        for (URL url : urls) {
            addUrl(url);
        }
    }


    /**
     * Load a possibly filtered path. Filters are defined
     * by using the * wildcard like in any shell.
     */
    private void loadFilteredPath(String filter) {
        List<URL> filesToLoad = LoaderConfigurationHelper.getFilesFromFilteredPath(filter);
        for (URL url : filesToLoad)
            addUrl(url);
    }


    /*
     * return true if the parent of the path inside the given
     * string does exist
     */
    private boolean parentPathDoesExist(String path) {
        File dir = new File(path).getParentFile();
        return dir.exists();
    }

    /*
     * separates the given path at the last '/'
     */
    private String getParentPath(String filter) {
        int index = filter.lastIndexOf('/');
        if (index == -1) return "";
        return filter.substring(index + 1);
    }

    /**
     * Adds a url to the classpath if it exists.
     *
     * @param url the url to add
     */
    public void addUrl(URL url) {
        if (url!= null) {
            if (url.getProtocol().equalsIgnoreCase("http")) {
                try {
                    HttpURLConnection huc =  ( HttpURLConnection ) url.openConnection();
                    huc.setRequestMethod("HEAD");
                    if (huc.getResponseCode() == HttpURLConnection.HTTP_OK)
                    classPath.add(url);
                } catch (ProtocolException e) {
                    logger.error("Problem with protocol while loading URL to classpath: " + url.toString() + "\n" + e.getMessage());
                } catch (IOException e) {
                    logger.error("Problem adding remote file to classpath, file does not exist: " + url.toString() + "\n" + e.getMessage());
                }
            } else if (url.getProtocol().equalsIgnoreCase("file")) {
                File jarFile = null;
                try {
                    jarFile = new File(url.toURI());
                } catch (URISyntaxException ue) {
                    jarFile = new File(url.getPath());
                }
                if (jarFile!=null && jarFile.exists())
                    classPath.add(url);
            } else if (url.getProtocol().equalsIgnoreCase("artifact")) {
                logger.debug("Ignoring artifact protocol - should already be resolved to http/file for: " + url.toString());
            } else {
                logger.error("Unsupported protocol to be added to classpath: " + url.toString());
            }
        }
    }

    /**
     * Adds a file to the classpath if it exists.
     *
     * @param file the file to add
     */
    public void addFile(File file) {
        if (file != null && file.exists()) {
            try {
                classPath.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new AssertionError("converting an existing file to an url should have never thrown an exception!");
            }
        }
    }

    /**
     * Adds a file to the classpath if it exists.
     *
     * @param filename the key of the file to add
     */
    public void addFile(String filename) {
        if (filename != null) addFile(new File(filename));
    }

    /**
     * Adds a classpath to this configuration. It expects a string with
     * multiple paths, separated by the system dependent path separator.
     * Expands wildcards, e.g. dir/* into all the jars in dir.
     *
     * @param path the path as a path separator delimited string
     * @see java.io.File#pathSeparator
     */
    public void addClassPath(String path) {
       String[] paths = path.split(File.pathSeparator);
        for (String cpPath : paths) {
            // Check to support wild card classpath
            if (cpPath.endsWith("*")) {
                File dir = new File(cpPath.substring(0, cpPath.length() - 1));
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".jar")) addFile(file);
                    }
                }
            } else {
                addFile(new File(cpPath));
            }
        }
    }

    /**
     * The classpath as URL[] from this configuration.
     * This can be used to construct a class loader.
     *
     * @return the classpath
     * @see java.net.URLClassLoader
     */
    public URL[] getClassPathUrls() {
        return (URL[]) classPath.toArray(new URL[classPath.size()]);
    }

    /**
     * Returns the key of the main class for this configuration.
     *
     * @return the key of the main class or null if not defined
     */
    public String getMainClass() {
        return main;
    }

    /**
     * Sets the main class. If there is already a main class
     * it is overwritten. Calling {@link #configure(InputStream)}
     * after calling this method does not require a main class
     * definition inside the stream.
     *
     * @param classname the key to become the main class
     */
    public void setMainClass(String classname) {
        main = classname;
        requireMain = false;
    }

    /**
     * Determines if a main class is required when calling.
     *
     * @param requireMain setValue to false if no main class is required
     * @see #configure(InputStream)
     */
    public void setRequireMain(boolean requireMain) {
        this.requireMain = requireMain;
    }

}
