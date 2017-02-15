/*
 * Copyright to the original author or authors.
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
package sorcer.data;

import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.tools.webster.Webster;
import sorcer.util.FileURLHandler;
import sorcer.util.GenericUtil;
import sorcer.util.JavaSystemProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The DataService provides the support to serve up data from a directory (or a setValue of directories).
 *
 * @author Dennis Reedy
 */
public class DataService implements FileURLHandler {
    private int port;
    private String[] roots;
    private final AtomicReference<Webster> websterRef = new AtomicReference<>();
    private String address;
    private static final Logger logger = LoggerFactory.getLogger(DataService.class.getName());
    public static final String DATA_DIR = "sorcer.data.dir";
    public static final String DATA_URL = "sorcer.data.url";
    private int minThreads = Webster.DEFAULT_MIN_THREADS;
    private int maxThreads = Webster.DEFAULT_MAX_THREADS;

    /**
     * Get the DataService that is bound to the platform code server.
     *
     * @return The DataService that is bound to the platform code server,
     * or if platform server properties are not found, return a DataService
     * using the default data dir and an anonymous port.
     */
    public static DataService getPlatformDataService() {
        DataService dataService;
        String webster = System.getProperty(DATA_URL, System.getProperty(Constants.WEBSTER));
        if(webster!=null) {
            int ndx = webster.lastIndexOf(":");
            int port = Integer.parseInt(webster.substring(ndx+1));
            String roots = getDataDir();
            dataService = new DataService(port, roots.split(";")).start();
        } else {
            logger.warn("Platform DataService property not found, " +
                        "create DataService using the data dir (" + getDataDir() + "), " +
                        "and an anonymous port");
            dataService = new DataService(0, getDataDir().split(";")).start();

            System.setProperty(DATA_URL, String.format("http://%s:%d", dataService.address, dataService.port));
        }
        return dataService;
    }

    /**
     * Create a DataService with roots. The resulting service will use an anonymous port.
     *
     * @param roots The roots to provide access to.
     */
    public DataService(final String... roots) {
        this(0, roots);
    }

    /**
     * Create a DataService with roots and a port.
     *
     * @param port The port to use.
     * @param roots The roots to provide access to.
     *
     * @throws IllegalArgumentException if the roots argument is empty or null, or if any of
     * the roots do not exist or are not directories.
     */
    public DataService(final int port, final String... roots) {
        this.port = port;
        if(roots==null || roots.length==0)
            throw new IllegalArgumentException("You must provide roots");
        List<String> adjusted = new ArrayList<>();
        for(String root : roots) {
            File f = new File(root);
            if(!f.exists())
                throw new IllegalArgumentException("The root ["+root+"] does not exist");
            if(!f.isDirectory())
                throw new IllegalArgumentException("The root ["+root+"] is not a directory");
            adjusted.add(root.replace('\\', '/'));
        }
        this.roots = adjusted.toArray(new String[adjusted.size()]);
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Start the data service if it has not been started yet.
     *
     * @return An updated instance of the DataService.
     */
    public DataService start()  {
        if(websterRef.get()==null) {
            StringBuilder websterRoots = new StringBuilder();
            for(String root : roots) {
                if (websterRoots.length() > 0)
                    websterRoots.append(";");
                websterRoots.append(root);
            }
            try {
                websterRef.set(new Webster(port, websterRoots.toString(), getDataDir(), minThreads, maxThreads));
                port = websterRef.get().getPort();
                address = websterRef.get().getAddress();
                logger.info(String.format("Started data service on: %s:%d\n%s",
                                          address, port, formatRoots()));
                writeRoots();
            } catch (IOException e) {
                try {
                    address = HostUtil.getInetAddressFromProperty(JavaSystemProperties.RMI_SERVER_HOSTNAME).getHostAddress();
                } catch (UnknownHostException e1) {
                    logger.error("Can not get host address", e1);
                    throw new RuntimeException("Can not get host address", e1);
                }
                if(!websterRoots.equals(getDefaultDataDir())) {
                    String derivedRoots = getRoots(port);
                    if(derivedRoots!=null)
                        roots = derivedRoots.split(";");
                }
                logger.warn(String.format("Data service already running, join %s:%d\n%s",
                                             address, port, formatRoots()));
            }
        }
        return this;
    }

    /**
     * Get a {@link URL} for a file path.
     *
     * @param path The file path to obtain a URL for.
     *
     * @return A URL that can be used to access the file.
     *
     * @throws IOException if the file does not exist, or the URL cannot be created.
     * @throws IllegalArgumentException if the file cannot be accessed from one of the roots provided.
     * @throws IllegalStateException if the data service is not running.
     */
    public URL getDataURL(final String path) throws IOException {
        return getDataURL(new File(path));
    }

    /**
     * Get a {@link URL} for a file.
     *
     * @param file The file to obtain a URL for.
     *
     * @return A URL that can be used to access the file.
     *
     * @throws IOException if the file does not exist, or the URL cannot be created.
     * @throws IllegalArgumentException if the file cannot be accessed from one of the roots provided.
     * @throws IllegalStateException if the data service is not available.
     */
    public URL getDataURL(final File file) throws IOException {
        return getDataURL(file, true);
    }

    /**
     * Get a {@link URL} for a file.
     *
     * @param file The file to obtain a URL for.
     * @param verify Whether to verify the file can be served up by the DataService and the
     * DataService is running.
     *
     * @return A URL that can be used to access the file.
     *
     * @throws IOException if the file does not exist, or the URL cannot be created.
     * @throws IllegalArgumentException if the file cannot be accessed from one of the roots provided.
     * @throws IllegalStateException if the data service is not available.
     */
    public URL getDataURL(final File file, final boolean verify) throws IOException {
        if(file==null)
            throw new IllegalArgumentException("The file argument cannot be null");
        if(!file.exists())
            throw new FileNotFoundException("The "+file.getPath()+" does not exist");
        if(address==null)
            throw new IllegalStateException("The data service is not available");
        String path = file.getPath().replace('\\', '/');
        String relativePath = null;
        for(String root : roots) {
            if(path.startsWith(root)) {
                relativePath = path.substring(root.length());
                break;
            }
        }
        if(relativePath==null)
            throw new IllegalArgumentException("The provided path ["+path+"], is not navigable " +
                    "from existing roots "+ Arrays.toString(roots));
        URL url =  new URL(String.format("http://%s:%d%s", address, port, relativePath));
        if(verify) {
            IOException notAvailable = verify(url);
            if (notAvailable != null) {
                logger.warn(String.format("Unable to verify %s, try and start DataService on %s:%d",
                                          url.toExternalForm(), address, port));
                start();
                notAvailable = verify(url);
                if (notAvailable != null)
                    throw notAvailable;
            }
        }
        return url;
    }

    /**
     * Download the contents of a URL to a local file
     *
     * @param url The URL to download
     * @param to The file to download to
     *
     * @throws IOException
     */
    public void download(final URL url, final File to) throws IOException {
        GenericUtil.download(url, to);
    }

    /**
     * Get a File from a URL.
     *
     * @param url The URL to use
     *
     * @return a File derived from the DataService data directory root(s).
     *
     * @throws FileNotFoundException If the URL cannot be accessed from one of the roots provided.
     */
    public File getDataFile(final URL url) throws IOException {
        File file = null;
        if(url.getProtocol().startsWith("file")) {
            try {
                File f = new File(url.toURI());
                for(String root : roots) {

                    // for matching windows paths (or any OS)
                    root = new File(root).getAbsolutePath();

                    if(f.getPath().startsWith(root)) {
                        file = f;
                        break;
                    }
                }
            } catch (URISyntaxException e) {
                throw new FileNotFoundException("Could not create file from "+url);
            }
        } else {
            String filePath = url.getPath();
            char sep = filePath.charAt(0);
            if (sep != File.separatorChar) {
                filePath = filePath.replace(sep, File.separatorChar);
            }
            for(String root : roots) {
                File f = new File(root, filePath);
                if(f.exists()) {
                    file = f;
                    break;
                }
            }
        }
        if(file==null || !file.exists())
            throw new FileNotFoundException("The "+url.toExternalForm()+" " +
                                            "is not accessible from existing roots "+ Arrays.toString(roots));
        return file;
    }

    /**
     * Stop the data service.
     */
    public void stop() {
        if(websterRef.get()!=null) {
            websterRef.get().terminate();
            websterRef.set(null);
        }
        address = null;
        getRootsFile(port).delete();
    }

    IOException verify(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.getResponseCode();
            connection.disconnect();
        } catch(IOException e) {
            return e;
        }
        return null;
    }

    private String formatRoots() {
        StringBuilder sb = new StringBuilder();
        int i=0;
        for(String root : roots) {
            if(sb.length()>0)
                sb.append("\n");
            sb.append("Root ").append(i++).append(" ").append(root);
        }
        return sb.toString();
    }

    /**
     * Get the DataService data directory. The {@link DataService#DATA_DIR} system property is first
     * consulted, if that property is not setValue, the default is either the TMPDIR
     * System.getProperty("java.io.tmpdir")/sorcer/user/data is used and the {@link DataService#DATA_DIR}
     * system property is setValue.
     *
     * @return The DataService data directory.
     */
    public static String getDataDir() {
        String dataDir = System.getProperty(DATA_DIR);
        if(dataDir==null) {
            dataDir = getDefaultDataDir();
            System.setProperty(DATA_DIR, dataDir);
        }
        return dataDir;
    }

    public static String getRoots(int port) {
        File rootsFile = getRootsFile(port);
        String roots = null;
        if(rootsFile.exists()) {
            try {
                roots = new String(Files.readAllBytes(rootsFile.toPath()));
            } catch (IOException e) {
                logger.error("Could not read {}", rootsFile.getPath(), e);
            }
        } else {
            if(logger.isTraceEnabled())
                logger.trace("roots file for {} does not exist", port);
        }
        return roots;
    }

    static File getRootsFile(int port) {
        File rootsDir = new File(getDefaultDataDir(), "roots");
        if(!rootsDir.exists())
            rootsDir.mkdirs();
        return new File(rootsDir, Integer.toString(port)+".roots");
    }

    void writeRoots() {
        File rootsFile = getRootsFile(port);
        rootsFile.deleteOnExit();
        StringBuilder websterRoots = new StringBuilder();
        for(String root : roots) {
            if (websterRoots.length() > 0)
                websterRoots.append(";");
            websterRoots.append(root);
        }
        try {
            Files.write(rootsFile.toPath(), websterRoots.toString().getBytes());
        } catch (IOException e) {
            logger.error("Could not save {}", rootsFile.getPath(), e);
        }
    }

    static String getDefaultDataDir() {
        String tmpDir = System.getenv("TMPDIR")==null?System.getProperty("java.io.tmpdir"):System.getenv("TMPDIR");
        return new File(String.format("%s%ssorcer-%s%sdata",
                                      tmpDir,
                                      File.separator,
                                      System.getProperty("user.name"),
                                      File.separator)).getAbsolutePath();
    }

    /**
     * Get the eval of the DATA_URL system property
     *
     * @return The eval of the DATA_URL system property
     */
    public String getDataUrl() {
        return String.format("http://%s:%d", address, port);
    }

    /**
     * Get the eval of the DATA_URL system property
     *
     * @return The eval of the DATA_URL system property
     */
    public String getDir() {
        return DataService.getDataDir();
    }
}
