/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package sorcer.tools.webster;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Webster is a HTTP server which can serve code from multiple codebases.
 * Environment variables used to control Webster are as follows:
 * <p/>
 * <table BORDER COLS=3 WIDTH="100%" >
 * <tr>
 * <td>sorcer.tools.webster.port</td>
 * <td>Sets the port for webster to use</td>
 * <td>0</td>
 * </tr>
 * <td>sorcer.tools.webster.root</td>
 * <td>Root directory to serve code from. Webster supports multiple root
 * directories which are separated by a <code>;</code></td>
 * <td>System.getProperty(user.home)</td>
 * </tr>
 * <p/>
 * </table>
 *
 * @author Dennis Reedy and Mike Sobolewski
 */
public class Webster implements Runnable {
    static final String BASE_COMPONENT = "sorcer.tools";
    static final String CODESERVER = BASE_COMPONENT + ".codeserver";

    static final int DEFAULT_MIN_THREADS = 1;
    static final int DEFAULT_MAX_THREADS = 10;
    private ServerSocket ss;
    private int port;
    private volatile boolean run = true;
    private static Properties MimeTypes = new Properties();
    private String[] websterRoot;
    private ThreadPoolExecutor pool;
    private int minThreads = DEFAULT_MIN_THREADS;
    private int maxThreads = DEFAULT_MAX_THREADS;
    private int startPort = 0;
    private int endPort = 0;
    private int soTimeout = 0;
    private static Logger logger = Logger.getLogger(Webster.class.getName());
    private com.sun.jini.start.LifeCycle lifeCycle;
    private boolean debug = false;
    private boolean isDaemon = false;
    private static String SERVER_DESCRIPTION = Webster.class.getName();
    private String tempDir;
    // Shared class server (webster) 
    private static Webster webster;

    /**
     * Create a new Webster. The port is determined by the
     * webster.port system property. If the
     * webster.port system property does not exist, an
     * anonymous port will be allocated.
     *
     * @throws BindException if Webster cannot create a socket
     */
    public Webster() throws BindException {
        String s = System.getProperty("webster.port");
        if (s != null && s != "0") {
            try {
                port = new Integer(s);
            } catch (NumberFormatException e) {
                if (s.equals("${webster.port}")) {
                    throw new RuntimeException("The required system property for 'webster.port' not set");
                }
            }
        } else {
            port = getAvailablePort();
        }
        this.isDaemon = false;
        initialize();
    }

    /**
     * Create a new Webster
     *
     * @param port The port to use
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port, boolean isDaemon) throws BindException {
        this.port = port;
        this.isDaemon = isDaemon;
        initialize();
    }

    /**
     * Create a new Webster
     *
     * @param roots The root(s) to serve code from. This is a semi-colin
     *              delimited list of directories
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(String roots, boolean isDaemon) throws BindException {
        String s = System.getProperty("webster.port");
        if (s != null) {
            port = new Integer(s);
        } else {
            port = getAvailablePort();
        }
        this.isDaemon = isDaemon;
        initialize(roots);
    }

    /**
     * Create a new Webster
     *
     * @param port  The port to use
     * @param roots The root(s) to serve code from. This is a semi-colin
     *              delimited list of directories
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port, String roots, boolean isDaemon) throws BindException {
        this.port = port;
        this.isDaemon = isDaemon;
        initialize(roots);
    }

    /**
     * Create a new Webster
     *
     * @param port        The port to use
     * @param roots       The root(s) to serve code from. This is a semi-colin
     *                    delimited list of directories
     * @param bindAddress TCP/IP address which Webster should bind to (null
     *                    implies no specific address)
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port, String roots, String bindAddress, boolean isDaemon)
            throws BindException {
        this.port = port;
        this.isDaemon = isDaemon;
        initialize(roots, bindAddress);
    }

    public Webster(int port, String[] roots, String bindAddress,
                   boolean isDaemon) throws BindException {
        this.port = port;
        this.isDaemon = isDaemon;
        initialize(roots, bindAddress);
    }

    /**
     * Create a new Webster
     *
     * @param port        The port to use
     * @param roots       The root(s) to serve code from. This is a semi-colin
     *                    delimited list of directories
     * @param bindAddress TCP/IP address which Webster should bind to (null
     *                    implies no specific address)
     * @param minThreads  Minimum threads to use in the ThreadPool
     * @param maxThreads  Minimum threads to use in the ThreadPool
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port,
                   String roots,
                   String bindAddress,
                   int minThreads,
                   int maxThreads,
                   boolean isDaemon) throws BindException {
        this.port = port;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.isDaemon = isDaemon;
        initialize(roots, bindAddress);
    }

    /**
     * Create a new Webster
     *
     * @param port        The port to use
     * @param roots       The root(s) to serve code from. This is a semi-colin
     *                    delimited list of directories
     * @param bindAddress TCP/IP address which Webster should bind to (null
     *                    implies no specific address)
     * @param minThreads  Minimum threads to use in the ThreadPool
     * @param maxThreads  Minimum threads to use in the ThreadPool
     * @param startPort   First port to try to listen
     * @param endPort     Last port to try to listen
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port,
                   String roots,
                   String bindAddress,
                   int minThreads,
                   int maxThreads,
                   int startPort,
                   int endPort,
                   boolean isDaemon) throws BindException {
        this.port = port;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.startPort = startPort;
        this.endPort = endPort;
        this.isDaemon = isDaemon;
        initialize(roots, bindAddress);
    }

    /**
     * Create a new Webster, compatible with the ServiceStarter mechanism in
     * Jini 2.0
     *
     * @param args      String[] of options. Valid options are [-port port],
     *                  [-roots list-of-roots], [-bindAddress address], [-minThreads minThreads],
     *                  [-maxThreads maxThreads] [-soTimeout soTimeout]
     * @param lifeCycle The LifeCycle object, may be null
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(String[] args, com.sun.jini.start.LifeCycle lifeCycle)
            throws Exception {
        if (args == null)
            throw new NullPointerException("Configuration is null");

        Webster.webster = this;
        this.lifeCycle = lifeCycle;
        String[] configRoots = null;
        String[] configArgs = null;
        String roots = null;
        String[] options = null;
        String bindAddress = null;
        if (args.length == 1 && new File(args[0]).isFile()) {
            final Configuration config = ConfigurationProvider.getInstance(args);
            try {
                configRoots = (String[]) config.getEntry(CODESERVER,
                                                         "roots", String[].class);
            } catch (Exception e) {
                e.printStackTrace();
                configRoots = null;
            }
            try {
                configArgs = (String[]) config.getEntry(CODESERVER,
                                                        "options", String[].class);
            } catch (Exception e) {
                e.printStackTrace();
                configArgs = null;
            }
//			logger.info("webster roots: " + Arrays.toString(configRoots));
//			logger.info("webster options: " + Arrays.toString(configArgs));
        }
        if (configRoots != null) {
            websterRoot = configRoots;
            options = configArgs;
        } else {
            options = args;
        }
        //logger.info("roots concat: " + roots + "\noptions: " + Arrays.toString(options));
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            if (option.equals("-port")) {
                i++;
                this.port = Integer.parseInt(options[i]);
            } else if (option.equals("-roots")) {
                i++;
                roots = options[i];
            } else if (option.equals("-bindAddress")) {
                i++;
                bindAddress = options[i];
            } else if (option.equals("-startPort")) {
                i++;
                startPort = Integer.parseInt(options[i]);
            } else if (option.equals("-endPort")) {
                i++;
                endPort = Integer.parseInt(options[i]);
            } else if (option.equals("-minThreads")) {
                i++;
                minThreads = Integer.parseInt(options[i]);
            } else if (option.equals("-maxThreads")) {
                i++;
                maxThreads = Integer.parseInt(options[i]);
            } else if (option.equals("-soTimeout")) {
                i++;
                soTimeout = Integer.parseInt(options[i]);
            } else if (option.equals("-isDaemon")) {
                i++;
                isDaemon = Boolean.parseBoolean(options[i]);
            } else if (option.equals("-debug")) {
                i++;
                debug = Boolean.parseBoolean(options[i]);
            } else {
                throw new IllegalArgumentException(option);
            }
        }
        if (websterRoot != null)
            init(bindAddress);
        else
            initialize(roots, bindAddress);

    }

    /*
     * Initialize Webster, serving code as determined by the either the
     * sorcer.tools.webster.root system property (if set) or defaulting to
     * the user.dir system property
     */
    private void initialize() throws BindException {
        String root = System.getProperty("webster.root");
        if (root == null)
            root = System.getProperty("user.dir");
        initialize(root);
    }

    /*
     * Initialize Webster
     * 
     * @param roots The root(s) to serve code from. This is a semicolon
     * delimited list of directories
     */
    private void initialize(String roots) throws BindException {
        initialize(roots, null);
    }

    private void initialize(String roots, String bindAddress) throws BindException {
        setupRoots(roots);
        init(bindAddress);
    }

    private void initialize(String[] roots, String bindAddress)
            throws BindException {
        websterRoot = roots;
        init(bindAddress);
    }

    /*
     * Initialize Webster
     *
     * @param roots The root(s) to serve code from. This is a semicolon
     * delimited list of directories
     */
    private void init(String bindAddress)
            throws BindException {
        String str = null;
        if (!debug) {
            str = System.getProperty("webster.debug");
            if (str != null && str.equals("true"))
                debug = true;
        }
        str = System.getProperty("webster.tmp.dir");
        if (str != null) {
            tempDir = str;
            if (debug)
                System.out.println("tempDir: " + tempDir);
        }

        for (int j = 0; j < websterRoot.length; j++) {
            if (debug) {
                System.out.println("Root " + j + " = " + websterRoot[j]);
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Root " + j + " = " + websterRoot[j]);
            }
        }

        InetAddress addr;
        try {
            if (bindAddress == null) {
                bindAddress = System.getProperty("webster.interface");
            }
            if (bindAddress == null)
                bindAddress = InetAddress.getLocalHost().getHostAddress();

            addr = InetAddress.getByName(bindAddress);
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Bind address server socket failure", e);
            return;
        }

        start(addr);
    }

    private void start(InetAddress addr) {
        if (port == 0) {
            try {
                port = getPortAvailable();
            } catch (IOException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Cannot determine a server socket port", e);
                System.exit(1);
            }
            startPort = port;
            endPort = port;
        } else {
            if (startPort == endPort) {
                startPort = port;
                endPort = port;
            }
        }

        for (int i = startPort; i <= endPort; i++) {
            try {
                start(i, addr);
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println(ex.getMessage());
            }
        }
    }

    // start with the first available port in the range STARTPORT-ENDPORT
    private void start(int websterPort, InetAddress address) throws IOException {
        try {
            port = websterPort;
            // check if the port is not required by the JVM system property
            String s = System.getProperty("webster.port");
            if (s != null && s.length() > 0) {
                port = new Integer(s);
            }
            ss = new ServerSocket(port, 0, address);
        } catch (IOException ioe) {
            if (startPort == endPort) {
                logger.log(Level.SEVERE, "Port bind server socket failure: " + endPort, ioe);
                System.exit(1);
            } else {
                System.err.println("Port bind server socket failure: " + port);
                throw ioe;
            }
        }
        port = ss.getLocalPort();

        if (debug)
            System.out.println("Webster serving on : "
                                       + ss.getInetAddress().getHostAddress() + ":" + port);
        if (logger.isLoggable(Level.INFO))
            logger.info("Webster serving on: "
                                + ss.getInetAddress().getHostAddress() + ":" + port);
        if (debug)
            System.out.println("Webster listening on port: " + port);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Webster listening on port: " + port);
        }
        try {
            pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
            if (debug)
                System.out.println("Webster minThreads [" + minThreads + "], "
                                           + "maxThreads [" + maxThreads + "]");
            if (logger.isLoggable(Level.FINE))
                logger.fine("Webster minThreads [" + minThreads + "], "
                                    + "maxThreads [" + maxThreads + "]");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not create ThreadPool", e);
            throw new RuntimeException("Could not create Thread Pool");
        }
        if (soTimeout > 0) {
            if (debug)
                System.out.println("Webster Socket SO_TIMEOUT set to ["
                                           + soTimeout + "] millis");
            if (logger.isLoggable(Level.FINE))
                logger.fine("Webster Socket SO_TIMEOUT set to [" + soTimeout
                                    + "] millis");
        }
        /* Set system property */
        System.setProperty(CODESERVER, "http://" + getAddress() + ":"
                + getPort());

        if (logger.isLoggable(Level.FINE))
            logger.fine("Webster isDaemon: " + isDaemon);

        Thread runner = new Thread(this, "Webster");
        if (isDaemon) {
            runner.setDaemon(true);
        }
        runner.start();
    }

    /**
     * Get the roots Webster is serving
     *
     * @return The roots Webster is serving as a semicolon delimited String
     */
    public String getRoots() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < websterRoot.length; i++) {
            if (i > 0)
                buffer.append(";");
            buffer.append(websterRoot[i]);
        }
        return (buffer.toString());
    }

    /**
     * Get address that Webster is bound to
     *
     * @return The host address the server socket Webster is using is bound to.
     *         If the socket is null, return null.
     */
    public String getAddress() {
        if (ss == null)
            return (null);
        return (ss.getInetAddress().getHostAddress());
    }

    /*
     * Setup the websterRoot property
     */
    private void setupRoots(String roots) {
        if (roots == null)
            throw new NullPointerException("roots is null");
        StringTokenizer tok = new StringTokenizer(roots, ";");
        websterRoot = new String[tok.countTokens()];
        if (websterRoot.length > 1) {
            for (int j = 0; j < websterRoot.length; j++) {
                websterRoot[j] = tok.nextToken();
                if (debug)
                    System.out.println("Root " + j + " = " + websterRoot[j]);
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Root " + j + " = " + websterRoot[j]);
            }
        } else {
            websterRoot[0] = roots;
            if (debug)
                System.out.println("Root  = " + websterRoot[0]);
            if (logger.isLoggable(Level.FINE))
                logger.fine("Root  = " + websterRoot[0]);
        }
    }

    /**
     * Terminate a running Webster instance
     */
    public void terminate() {
        run = false;
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException e) {
                logger.warning("Exception closing Webster ServerSocket");
            }
        }
        if (lifeCycle != null)
            lifeCycle.unregister(this);

        if (pool != null)
            pool.shutdownNow();
    }

    /**
     * Get the port Webster is bound to
     *
     * @return The port Webster is bound to
     */
    public int getPort() {
        return getAvailablePort();
    }

    private String readRequest(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        int read;
        int prev = -1;
        while ((read = inputStream.read()) != -1) {
            if (read != '\n' && read != '\r')
                sb.append((char) read);
            if (read == '\n' && prev == '\r') {
                break;
            }
            if (read == '\r' && prev == '0') {
                //sb.delete(0, sb.length());
                break;
            }
            prev = read;
        }
        return sb.toString();
    }


    public void run() {
        Socket s;
        try {
            loadMimes();
            String fileName;
            while (run) {
                s = ss.accept(); // accept incoming requests
                if (soTimeout > 0) {
                    s.setSoTimeout(soTimeout);
                }
                String line;
                Properties header = new Properties();
                DataInputStream inputStream = null;
                try {
                    inputStream = new DataInputStream(s.getInputStream());
                    StringBuilder lineBuilder = new StringBuilder();
                    StringTokenizer tokenizer;
                    while ((line = readRequest(inputStream)).length() != 0) {
                        if (lineBuilder.length() > 0)
                            lineBuilder.append("\n");
                        lineBuilder.append(line);
                        tokenizer = new StringTokenizer(line, ":");
                        String aToken = tokenizer.nextToken().trim();
                        if (tokenizer.hasMoreTokens()) {
                            header.setProperty(aToken, tokenizer.nextToken().trim());
                        }
                    }
                    line = lineBuilder.toString();
                    int port = s.getPort();
                    String from = s.getInetAddress().getHostAddress() + ":" + port;
                    if (debug) {
                        StringBuilder buff = new StringBuilder();
                        buff.append("From: ").append(from).append(", ");
                        if (soTimeout > 0)
                            buff.append("SO_TIMEOUT: ").append(soTimeout).append(", ");
                        buff.append("Request: ").append(line);
                        System.out.println("\n"+buff.toString());
                    }
                    if (logger.isLoggable(Level.FINE)) {
                        StringBuilder buff = new StringBuilder();
                        buff.append("From: ").append(from).append(", ");
                        if (soTimeout > 0)
                            buff.append("SO_TIMEOUT: ").append(soTimeout).append(", ");
                        buff.append("Request: ").append(line);
                        if (logger.isLoggable(Level.FINE))
                            logger.fine(buff.toString());
                    }
                    if (line != null && line.length()>0) {
                        tokenizer = new StringTokenizer(line, " ");
                        if (!tokenizer.hasMoreTokens())
                            break;
                        String token = tokenizer.nextToken();
                        fileName = tokenizer.nextToken();
                        if (fileName.startsWith("/"))
                            fileName = fileName.substring(1);
                        if (token.equals("GET")) {
                            header.setProperty("GET", fileName);
                        } else if (token.equals("PUT")) {
                            header.setProperty("PUT", fileName);
                        } else if (token.equals("DELETE")) {
                            header.setProperty("DELETE", fileName);
                        } else if (token.equals("HEAD")) {
                            header.setProperty("HEAD", fileName);
                        }
                        while (tokenizer.hasMoreTokens()) {
                            String aToken = tokenizer.nextToken().trim();
                            if (tokenizer.hasMoreTokens()) {
                                header.setProperty(aToken,
                                                   tokenizer.nextToken().trim());
                            }
                        }
                        if (header.getProperty("GET") != null) {
                            pool.execute(new GetFile(s, fileName));
                        } else if (header.getProperty("PUT") != null) {
                            pool.execute(new PutFile(s, fileName, header, inputStream));
                        } else if (header.getProperty("DELETE") != null) {
                            pool.execute(new DelFile(s, fileName));
                        } else if (header.getProperty("HEAD") != null) {
                            pool.execute(new Head(s, fileName));
                        } else {
                            if (debug)
                                System.out.println(
                                        "bad request [" + line + "] from " + from);
                            if (logger.isLoggable(Level.FINE))
                                logger.log(Level.FINE,
                                           "bad request [" + line + "] " +
                                                   "from " + from);
                            DataOutputStream clientStream =
                                    new DataOutputStream(
                                            new BufferedOutputStream(
                                                    s.getOutputStream()));
                            clientStream.writeBytes(
                                    "HTTP/1.0 400 Bad Request\r\n\r\n");
                            clientStream.flush();
                            clientStream.close();
                        }
                    } /* if line != null */
                } catch (Exception e) {
                    DataOutputStream clientStream =
                            new DataOutputStream(
                                    new BufferedOutputStream(
                                            s.getOutputStream()));
                    clientStream.writeBytes(
                            "HTTP/1.0 500 Internal Server Error\n" +
                                    "MIME-Version: 1.0\n" +
                                    "Server: " + SERVER_DESCRIPTION + "\n" +
                                    "\n\n<H1>500 Internal Server Error</H1>\n"
                                    + e);
                    clientStream.flush();
                    clientStream.close();
                    inputStream.close();
                    logger.log(Level.WARNING, "Getting Request", e);
                }
            }
        } catch (Exception e) {
            if (run) {
                e.printStackTrace();
                logger.log(Level.WARNING, "Processing HTTP Request", e);
            }
        }
    }

    /**
     * Get an anonymous port
     *
     * @return An available port created by instantiating a
     *         <code>java.net.ServerSocket</code> with a port of 0
     * @throws IOException If an available port cannot be obtained
     */
    public int getPortAvailable() throws java.io.IOException {
        java.net.ServerSocket socket = new java.net.ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    // load the properties file
    void loadMimes() throws IOException {
        if (debug)
            System.out.println("Loading mimetypes ... ");
        if (logger.isLoggable(Level.FINE))
            logger.fine("Loading mimetypes ... ");
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        URL fileURL =
                ccl.getResource("sorcer/tools/webster/mimetypes.properties");
        if (fileURL != null) {
            try {
                InputStream is = fileURL.openStream();
                MimeTypes.load(is);
                is.close();
                if (debug)
                    System.out.println("Mimetypes loaded");
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Mimetypes loaded");
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Loading Mimetypes", ioe);
            }
        } else {
            if (debug)
                System.out.println("mimetypes.properties not found, " +
                                           "loading defaults");
            if (logger.isLoggable(Level.FINE))
                logger.fine("mimetypes.properties not found, loading defaults");
            MimeTypes.put("jpg", "image/jpg");
            MimeTypes.put("jpeg", "image/jpg");
            MimeTypes.put("jpe", "image/jpg");
            MimeTypes.put("gif", "image/gif");
            MimeTypes.put("htm", "text/html");
            MimeTypes.put("html", "text/html");
            MimeTypes.put("txt", "text/plain");
            MimeTypes.put("qt", "video/quicktime");
            MimeTypes.put("mov", "video/quicktime");
            MimeTypes.put("class", "application/octet-stream");
            MimeTypes.put("mpg", "video/mpeg");
            MimeTypes.put("mpeg", "video/mpeg");
            MimeTypes.put("mpe", "video/mpeg");
            MimeTypes.put("au", "audio/basic");
            MimeTypes.put("snd", "audio/basic");
            MimeTypes.put("wav", "audio/x-wave");
            MimeTypes.put("JNLP", "application/x-java-jnlp-file");
            MimeTypes.put("jnlp", "application/x-java-jnlp-file");
            MimeTypes.put("java", "application/java");
            MimeTypes.put("jar", "application/java");
            MimeTypes.put("JAR", "application/java");
        }
    } // end of loadMimes

    protected File parseFileName(String filename) {
        StringBuffer fn = new StringBuffer(filename);
        for (int i = 0; i < fn.length(); i++) {
            if (fn.charAt(i) == '/')
                fn.replace(i, i + 1, File.separator);
        }
        File f = null;
        String[] roots = expandRoots();
        for (String root : roots) {

            f = new File(root, fn.toString());
            //System.out.println("root: "+root+", looking for "+f.getPath());
            if (f.exists()) {
                return (f);
            }
        }
        return (f);
    }

    protected String[] expandRoots() {
        List<String> expandedRoots = new LinkedList<String>();
        if (hasWildcard()) {
            String[] rawRoots = websterRoot;
            for (String root : rawRoots) {
                int wildcard;
                if ((wildcard = root.indexOf('*')) != -1) {
                    String prefix = root.substring(0, wildcard);
                    File prefixFile = new File(prefix);
                    if (prefixFile.exists()) {
                        String suffix =
                                (wildcard < (root.length() - 1)) ?
                                        root.substring(wildcard + 1) : "";
                        String[] children = prefixFile.list();
                        for (String aChildren : children) {
                            expandedRoots.add(prefix + aChildren + suffix);
                        }
                    } else {
                        // Eat the root entry if it's wildcarded and doesn't
                        // exist
                    }
                } else {
                    expandedRoots.add(root);
                }
            }
        }
        String[] roots;
        if (expandedRoots.size() > 0) {
            roots = expandedRoots.toArray(new String[expandedRoots.size()]);
        } else {
            roots = websterRoot;
        }
        return (roots);
    }

    /*
     * See if the root is using a wildcard
     */
    boolean hasWildcard() {
        boolean wildcarded = false;
        for (String root : websterRoot) {
            if ((root.indexOf('*')) != -1) {
                wildcarded = true;
                break;
            }
        }
        return (wildcarded);
    }

    class Head implements Runnable {
        private Socket client;
        private String fileName;

        Head(Socket s, String fileName) {
            client = s;
            this.fileName = fileName;
        }

        public void run() {
            StringBuffer dirData = new StringBuffer();
            StringBuffer logData = new StringBuffer();
            try {
                File getFile = parseFileName(fileName);
                logData.append("Do HEAD: input=")
                       .append(fileName)
                       .append(", " + "parsed=")
                       .append(getFile)
                       .append(", ");
                int fileLength;
                String header;
                if (getFile.isDirectory()) {
                    logData.append("directory located");
                    String files[] = getFile.list();
                    for (String file : files) {
                        File f = new File(getFile, file);
                        dirData.append(f.toString().substring(
                                getFile.getParent().length()));
                        dirData.append("\t");
                        if (f.isDirectory())
                            dirData.append("d");
                        else
                            dirData.append("f");
                        dirData.append("\t");
                        dirData.append(f.length());
                        dirData.append("\t");
                        dirData.append(f.lastModified());
                        dirData.append("\n");
                    }
                    fileLength = dirData.length();
                    String fileType = MimeTypes.getProperty("txt");
                    if (fileType == null)
                        fileType = "application/java";
                    header = "HTTP/1.0 200 OK\n" +
                            "Allow: GET\nMIME-Version: 1.0\n" +
                            "Server: " + SERVER_DESCRIPTION + "\n" +
                            "Content-Type: " + fileType + "\n" +
                            "Content-Length: " + fileLength + "\r\n\r\n";
                } else if (getFile.exists()) {
                    DataInputStream requestedFile = new DataInputStream(
                            new BufferedInputStream(new FileInputStream(getFile)));
                    fileLength = requestedFile.available();
                    String fileType =
                            fileName.substring(fileName.lastIndexOf(".") + 1,
                                               fileName.length());
                    fileType = MimeTypes.getProperty(fileType);
                    logData.append("file size: [").append(fileLength).append("]");
                    header = "HTTP/1.0 200 OK\n"
                            + "Allow: GET\nMIME-Version: 1.0\n"
                            + "Server: " + SERVER_DESCRIPTION + "\n"
                            + "Content-Type: "
                            + fileType
                            + "\n"
                            + "Content-Length: "
                            + fileLength
                            + "\r\n\r\n";
                } else {
                    header = "HTTP/1.1 404 Not Found\r\n\r\n";
                    logData.append("not found");
                }

                if (debug)
                    System.out.println(logData.toString());
                if (logger.isLoggable(Level.FINE))
                    logger.fine(logData.toString());

                DataOutputStream clientStream =
                        new DataOutputStream(
                                new BufferedOutputStream(client.getOutputStream()));
                clientStream.writeBytes(header);
                clientStream.flush();
                clientStream.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing Socket", e);
            } finally {
                try {
                    client.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING,
                               "Closing incoming socket",
                               e2);
                }
            }
        } // end of Head
    }

    class GetFile implements Runnable {
        private Socket client;
        private String fileName;
        private DataInputStream requestedFile;
        private int fileLength;

        GetFile(Socket s, String fileName) {
            client = s;
            this.fileName = fileName;
        }

        public void run() {
            StringBuffer dirData = new StringBuffer();
            StringBuffer logData = new StringBuffer();
            try {
                File getFile = parseFileName(fileName);
                logData.append("Do GET: input=")
                       .append(fileName)
                       .append(", " + "parsed=")
                       .append(getFile)
                       .append(", ");
                String header;
                if (getFile.isDirectory()) {
                    logData.append("directory located");
                    String files[] = getFile.list();
                    for (String file : files) {
                        File f = new File(getFile, file);
                        dirData.append(f.toString().substring(
                                getFile.getParent().length()));
                        dirData.append("\t");
                        if (f.isDirectory())
                            dirData.append("d");
                        else
                            dirData.append("f");
                        dirData.append("\t");
                        dirData.append(f.length());
                        dirData.append("\t");
                        dirData.append(f.lastModified());
                        dirData.append("\n");
                    }
                    fileLength = dirData.length();
                    String fileType = MimeTypes.getProperty("txt");
                    if (fileType == null)
                        fileType = "application/java";
                    header = "HTTP/1.0 200 OK\n"
                            + "Allow: GET\nMIME-Version: 1.0\n"
                            + "Server: " + SERVER_DESCRIPTION + "\n"
                            + "Content-Type: "
                            + fileType
                            + "\n"
                            + "Content-Length: "
                            + fileLength
                            + "\r\n\r\n";
                } else if (getFile.exists()) {
                    requestedFile =
                            new DataInputStream(
                                    new BufferedInputStream(new FileInputStream(getFile)));
                    fileLength = requestedFile.available();
                    String fileType =
                            fileName.substring(fileName.lastIndexOf(".") + 1,
                                               fileName.length());
                    fileType = MimeTypes.getProperty(fileType);
                    header = "HTTP/1.0 200 OK\n"
                            + "Allow: GET\nMIME-Version: 1.0\n"
                            + "Server: " + SERVER_DESCRIPTION + "\n"
                            + "Content-Type: "
                            + fileType
                            + "\n"
                            + "Content-Length: "
                            + fileLength
                            + "\r\n\r\n";
                } else {
                    header = "HTTP/1.0 404 Not Found\r\n\r\n";
                }
                DataOutputStream clientStream =
                        new DataOutputStream(
                                new BufferedOutputStream(client.getOutputStream()));
                clientStream.writeBytes(header);

                if (getFile.isDirectory()) {
                    clientStream.writeBytes(dirData.toString());
                } else if (getFile.exists()) {
                    byte[] buffer = new byte[fileLength];
                    requestedFile.readFully(buffer);
                    logData.append("file size: [").append(fileLength).append("]");
                    try {
                        clientStream.write(buffer);
                    } catch (Exception e) {
                        String s = "Sending [" +
                                getFile.getAbsolutePath() + "], " +
                                "size [" + fileLength + "], " +
                                "to client at " +
                                "[" +
                                client.getInetAddress().getHostAddress() +
                                "]";
                        if (logger.isLoggable(Level.FINE))
                            logger.log(Level.FINE, s, e);
                        if (debug) {
                            System.out.println(s);
                            e.printStackTrace();
                        }
                    }
                    requestedFile.close();
                } else {
                    logData.append("not found");
                }
                if (debug)
                    System.out.println(logData.toString());
                if (logger.isLoggable(Level.FINE))
                    logger.fine(logData.toString());
                clientStream.flush();
                clientStream.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Closing Socket", e);
            } finally {
                try {
                    client.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING,
                               "Closing incoming socket",
                               e2);
                }
            }
        } // end of GetFile
    }

    class PutFile implements Runnable {
        private Socket client;
        private String fileName;
        private Properties rheader;
        private InputStream inputStream;
        final int BUFFER_SIZE = 4096;

        PutFile(Socket s, String fileName, Properties header, InputStream fromClient) {
            rheader = header;
            client = s;
            this.fileName = fileName;
            this.inputStream = fromClient;
        }

        public void run() {

            String s = ignoreCaseProperty(rheader, "Content-Length");
            if (s == null) {
                try {
                    sendResponse("HTTP/1.0 411 OK\n"
                                         + "Allow: PUT\n"
                                         + "MIME-Version: 1.0\n"
                                         + "Server : SORCER Webster: a Java HTTP Server \n"
                                         + "\n\n <H1>411 Webster refuses to accept the out request for " + fileName + " " +
                                         "without a defined Content-Length.</H1>\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String header;
                OutputStream requestedFileOutputStream = null;
                try {
                    // check to see if the file exists if it does the return code
                    // will be 200 if it doesn't it will be 201
                    File putFile;
                    if (tempDir != null) {
                        putFile = new File(tempDir + File.separator + fileName);
                    } else {
                        putFile = parseFileName(fileName);
                    }
                    if (debug)
                        System.out
                              .println("tempDir: " + tempDir + ", fileName: " + fileName + ", putFile: " + putFile.getPath());

                    if (putFile.exists()) {
                        header = "HTTP/1.0 200 OK\n"
                                + "Allow: PUT\n"
                                + "MIME-Version: 1.0\n"
                                + "Server : SORCER Webster: a Java HTTP Server \n"
                                + "\n\n <H1>200 PUT File " + fileName + " updated</H1>\n";
                        if (debug)
                            System.out.println("updated putFile: " + putFile);
                    } else {
                        header = "HTTP/1.0 201 Created\n"
                                + "Allow: PUT\n"
                                + "MIME-Version: 1.0\n"
                                + "Server : SORCER Webster: a Java HTTP Server \n"
                                + "\n\n <H1>201 PUT File " + fileName + " Created</H1>\n";
                        File parentDir = putFile.getParentFile();
                        System.out.println("Parent: " + parentDir.getPath() + ", exists? " + parentDir.exists());
                        if (!parentDir.exists()) {
                            if (parentDir.mkdirs() && debug) {
                                System.out.println("Created " + parentDir.getPath());
                            }
                        }
                        if (debug)
                            System.out.println("Created putFile: " + putFile + ", exists? " + putFile.exists());
                    }

                    int length = Integer.parseInt(ignoreCaseProperty(rheader, "Content-Length"));
                    if (debug)
                        System.out.println("Putting " + fileName + " size: " + length + ", header: " + rheader);
                    try {
                        requestedFileOutputStream = new DataOutputStream(new FileOutputStream(putFile));
                        int read;
                        long amountRead = 0;
                        byte[] buffer = new byte[length < BUFFER_SIZE ? length : BUFFER_SIZE];
                        while (amountRead < length) {
                            read = inputStream.read(buffer);
                            requestedFileOutputStream.write(buffer, 0, read);
                            amountRead += read;
                        }
                        requestedFileOutputStream.flush();
                        System.out.println("Wrote: " + putFile.getPath() + " size: " + putFile.length());
                    } catch (IOException e) {
                        e.printStackTrace();
                        header = "HTTP/1.0 500 Internal Server Error\n"
                                + "Allow: PUT\n"
                                + "MIME-Version: 1.0\n"
                                + "Server: " + SERVER_DESCRIPTION + "\n"
                                + "\n\n <H1>500 Internal Server Error</H1>\n"
                                + e;
                    } finally {
                        if (requestedFileOutputStream != null)
                            requestedFileOutputStream.close();
                        sendResponse(header);
                    }

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Closing Socket", e);
                } finally {
                    try {
                        if (requestedFileOutputStream != null)
                            requestedFileOutputStream.close();
                        client.close();
                    } catch (IOException e2) {
                        logger.log(Level.WARNING, "Closing incoming socket", e2);
                    }
                }
            }
        }

        void sendResponse(String header) throws IOException {
            DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
            clientStream.writeBytes(header);
            clientStream.flush();
            clientStream.close();
        }

        public String ignoreCaseProperty(Properties props, String field) {
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                String propName = (String) names.nextElement();
                if (field.equalsIgnoreCase(propName)) {
                    return (props.getProperty(propName));
                }
            }
            return (null);
        }
    } // end of PutFile

    class DelFile implements Runnable {
        private Socket client;
        private String fileName;

        DelFile(Socket s, String fileName) {
            client = s;
            this.fileName = fileName;
        }

        public void run() {
            try {
                File putFile = parseFileName(fileName);
                String header;
                if (!putFile.exists()) {
                    header = "HTTP/1.0 404 File not found\n"
                            + "Allow: GET\n"
                            + "MIME-Version: 1.0\n"
                            + "Server: " + SERVER_DESCRIPTION + "\n"
                            + "\n\n <H1>404 File not Found</H1>\n"
                            + "<BR>";
                } else if (putFile.delete()) {
                    header = "HTTP/1.0 200 OK\n"
                            + "Allow: PUT\n"
                            + "MIME-Version: 1.0\n"
                            + "Server: " + SERVER_DESCRIPTION + "\n"
                            + "\n\n <H1>200 File succesfully deleted</H1>\n";
                } else {
                    header = "HTTP/1.0 500 Internal Server Error\n"
                            + "Allow: PUT\n"
                            + "MIME-Version: 1.0\n"
                            + "Server: " + SERVER_DESCRIPTION + "\n"
                            + "\n\n <H1>500 File could not be deleted</H1>\n";
                }
                DataOutputStream clientStream =
                        new DataOutputStream(
                                new BufferedOutputStream(client.getOutputStream()));
                clientStream.writeBytes(header);
                clientStream.flush();
                clientStream.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Closing Socket", e);
            } finally {
                try {
                    client.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING,
                               "Closing incoming socket",
                               e2);
                }
            }
        }
    }

    public static int getWebsterPort() {
        return new Integer(System.getProperty("webster.port"));
    }

    public int getAvailablePort() {
        if (port == 0) {
            java.net.ServerSocket socket;
            try {
                socket = new java.net.ServerSocket(0);
                port = socket.getLocalPort();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return port;
    }

    public static void main(String[] args) {
        try {
            new Webster();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * Returns the embedded class server (webster) for this environment.
     * </p>
     *
     * @return the embedded class server
     */
    public static Webster getWebster() {
        return webster;
    }
}
