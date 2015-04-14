package sorcer.netlet;

import groovy.lang.GroovyRuntimeException;
import net.jini.config.Configuration;
import org.codehaus.groovy.control.CompilationFailedException;
import sorcer.netlet.util.LoaderConfigurationHelper;
import sorcer.netlet.util.ScriptExertException;
import sorcer.resolver.Resolver;
import sorcer.netlet.util.ScriptThread;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sorcer Script Exerter - this class handles parsing an ntl (Netlet) script and executing it or returning its
 * content as an object
 * <p/>
 * User: prubach
 * Date: 02.07.13
 */
public class ScriptExerter {

    private final static Logger logger = LoggerFactory.getLogger(ScriptExerter.class
            .getName());

    private final static String LINE_SEP = "\n";

    private final static String SHELL_LINE = "#!";

    private final static String[] GROOVY_ERR_MSG_LINE = new String[]{"groovy: ", "@ line "};

    private boolean startsWithShellLine = false;

    private String input;

    private PrintStream out;

    private File outputFile;

    private File scriptFile;

    private String script;

    private List<String> loadLines = new ArrayList<String>();

    private List<String> codebaseLines = new ArrayList<String>();

    private ClassLoader classLoader;

    private static StringBuilder staticImports;

    private Object target;

    private Object result;

    private ScriptThread scriptThread;

    private String websterStrUrl;

    private Configuration config;

    private Set<URL> urlsToLoad = new HashSet<URL>();

    private boolean debug = false;

    public ScriptExerter() {
        this(null, null, null, false);
    }

    public final static long startTime = System.currentTimeMillis();

    public static String[] localJars = new String[] {
            //"org.sorcersoft.sorcer:sos-api"
    };

    public ScriptExerter(PrintStream out, ClassLoader classLoader, String websterStrUrl, boolean debug) {
        this.out = out;
        if (out==null) this.out = System.out;
        this.debug = debug;
        this.classLoader = classLoader;
        try {
            for (String jar : localJars) {
                File f = new File(Resolver.resolveAbsolute(jar));
                urlsToLoad.add(f.toURI().toURL());
            }
        } catch (MalformedURLException me) {
            out.println("Problem loading default classpath for scripts in ScriptExerter: " + me);
        }
        this.websterStrUrl = websterStrUrl;
        if (staticImports == null) {
            staticImports = readTextFromJar("static-imports.txt");
        }
    }

    public ScriptExerter(File scriptFile) throws IOException {
        this(scriptFile, null, null, null);
    }

    public ScriptExerter(File scriptFile, PrintStream out, ClassLoader classLoader, String websterStrUrl) throws IOException {
        this(out, classLoader, websterStrUrl, false);
        this.scriptFile = scriptFile;
        readFile(scriptFile);
    }

    public ScriptExerter(String script, PrintStream out, ClassLoader classLoader, String websterStrUrl) throws IOException {
        this(out, classLoader, websterStrUrl, false);
        readScriptWithHeaders(script);
    }


    public Object execute() throws Throwable {
        if (scriptThread != null) {
            scriptThread.run();
            result = scriptThread.getResult();
            return result;
        }
        throw new ScriptExertException("You must first call parse() before calling execute() ");
    }

    public Object parse() throws Throwable {
        // Process "load" and generate a list of URLs for the classloader
        if (out!=null) out.println("parsing..." + (System.currentTimeMillis()-startTime) +"ms");

        if (!loadLines.isEmpty()) {
            for (String jar : loadLines) {
                String loadPath = jar.substring(LoaderConfigurationHelper.LOAD_PREFIX.length()).trim();
                urlsToLoad.addAll(LoaderConfigurationHelper.load(loadPath));
            }
        }
        // Process "codebase" and set codebase variable
        if (out!=null) out.println("setting codebase..."+ (System.currentTimeMillis()-startTime)+"ms");
        LoaderConfigurationHelper.setCodebase(codebaseLines, out);

        if (out!=null) out.println("loading codebase urls..."+ (System.currentTimeMillis()-startTime)+"ms");
        // resolve codebase and add to classpath
        for (String codebaseStr : codebaseLines) {
            if (codebaseStr.startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX))
                codebaseStr = codebaseStr.substring(LoaderConfigurationHelper.CODEBASE_PREFIX.length()).trim();
            urlsToLoad.addAll(LoaderConfigurationHelper.load(codebaseStr));
        }

        try {
            if (out!=null) out.println("creating scriptThread..."+ (System.currentTimeMillis()-startTime)+"ms");
            scriptThread = new ScriptThread(script,
                    new URLClassLoader(urlsToLoad.toArray(new URL[0]), (classLoader!=null ? classLoader : getClass().getClassLoader())),
                    out, config, debug);
            if (out!=null) out.println("get target..." + (System.currentTimeMillis()-startTime)+"ms");
            this.target = scriptThread.getTarget();
            return target;
        }
        // Parse Groovy errors and replace line numbers to adjust according to show the actual line number with an error
        catch (GroovyRuntimeException e) {
            int lineNum = 0;
            if (e instanceof CompilationFailedException) {
                String msg = e.getMessage();
                for (String groovyErrMsg : GROOVY_ERR_MSG_LINE) {
                    lineNum = findLineNumAfterText(msg, groovyErrMsg);
                    if (lineNum > 0) msg = msg.replace(groovyErrMsg + lineNum,
                            groovyErrMsg + (lineNum - getLineOffsetForGroovyErrors()));
                }
                throw new ScriptExertException(msg, e, lineNum - getLineOffsetForGroovyErrors());
            }
            for (StackTraceElement st : e.getStackTrace()) {
                if (st.getClassName().equals("Script1")) {
                    lineNum = st.getLineNumber();
                    break;
                }
            }
            throw new ScriptExertException(e.getLocalizedMessage(), e, lineNum - getLineOffsetForGroovyErrors());
        } catch (Exception e) {
            logger.error( "error while parsing", e);
            throw new ScriptExertException(e.getLocalizedMessage(), e, -getLineOffsetForGroovyErrors());
        }
    }


    public void readFile(File file) throws IOException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        List<String> loadLines = new ArrayList<String>();
        List<String> codebaseLines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String nextLine = "";
        StringBuffer sb = new StringBuffer();
        sb.append(staticImports.toString());
        nextLine = br.readLine();
        if (nextLine.indexOf(SHELL_LINE) < 0) {
            sb.append(nextLine).append(LINE_SEP);
        } else
            startsWithShellLine = true;
        while ((nextLine = br.readLine()) != null) {
            // Check for "load" of jars
            if (nextLine.trim().startsWith(LoaderConfigurationHelper.LOAD_PREFIX)) {
                this.loadLines.add(nextLine.trim());
            } else if (nextLine.trim().startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX)) {
                this.codebaseLines.add(nextLine.trim());
            } else {
                sb.append(nextLine.trim()).append(LINE_SEP);
            }
        }
        this.script = sb.toString();
    }

    public void readScriptWithHeaders(String script) throws IOException {
        String[] lines = script.split(LINE_SEP);
        StringBuilder sb = new StringBuilder(staticImports.toString());
        for (String line : lines) {
            // Check for "load" of jars
            if (line.trim().startsWith(LoaderConfigurationHelper.LOAD_PREFIX)) {
                this.loadLines.add(line.trim());
            } else if (line.trim().startsWith(LoaderConfigurationHelper.CODEBASE_PREFIX)) {
                this.codebaseLines.add(line.trim());
            } else if (!line.startsWith(SHELL_LINE)) {
                sb.append(line);
                sb.append(LINE_SEP);
            } else
                startsWithShellLine = true;
        }
        this.script = sb.toString();
    }

    private StringBuilder readTextFromJar(String filename) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        StringBuilder sb = new StringBuilder();

        try {
            is = getClass().getClassLoader().getResourceAsStream(filename);
            logger.debug("Loading " + filename + " from is: " + is);
            if (is != null) {
                br = new BufferedReader(new InputStreamReader(is));
                while (null != (line = br.readLine())) {
                    sb.append(line);
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb;
    }

    public List<String> getLoadLines() {
        return loadLines;
    }

    public void setLoadLines(List<String> loadLines) {
        this.loadLines = loadLines;
    }

    public List<String> getCodebaseLines() {
        return codebaseLines;
    }

    public void setCodebaseLines(List<String> codebaseLines) {
        this.codebaseLines = codebaseLines;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Object getTarget() {
        return target;
    }

    public Object getResult() {
        return result;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public int getLineOffsetForGroovyErrors() {
        int staticLines = staticImports.toString().split("\n", -1).length - 1;
        return staticLines - codebaseLines.size() - (startsWithShellLine ? 1 : 0);
    }

    public int findLineNumAfterText(String msg, String needle) {
        Pattern p = Pattern.compile(needle + "+([0-9]+).*");
        Matcher m = p.matcher(msg);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }
}


