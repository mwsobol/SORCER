package sorcer.netlet;

import groovy.lang.GroovyRuntimeException;
import net.jini.config.Configuration;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.rioproject.RioVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.netlet.util.NetletClassLoader;
import sorcer.netlet.util.ScriptExertException;
import sorcer.netlet.util.ScripterThread;
import sorcer.service.Routine;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sorcer Netlet Scripter - this class handles parsing an ntl (Netlet) script and executing it or returning its
 * content as an object
 * <p/>
 * User: prubach & Mile Sobolewski
 * Date: 02.07.13
 */
public class ServiceScripter {

    private final static Logger logger = LoggerFactory.getLogger(ServiceScripter.class
            .getName());


    private final static String[] GROOVY_ERR_MSG_LINE = new String[]{"groovy: ", "@ line "};

    private PrintStream out;

    private String script;

    private NetletClassLoader classLoader;

    private Object target;

    private Object result;

    private ScripterThread scripterThread;

    private ServiceShell serviceShell;

    private String websterStrUrl;

    private Configuration config;

    private boolean isExertable = true;

    private boolean debug = false;

    public ServiceScripter() {
        this(null, null, null, false);
    }

    public final static long startTime = System.currentTimeMillis();

    final protected static URL[] defaultCodebase;

    static {
        try {
            defaultCodebase = new URL[] {
                new URL("artifact:org.rioproject/rio-api/" + RioVersion.VERSION)
            };
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ServiceScripter(PrintStream out, ClassLoader classLoader, String websterStrUrl, boolean debug) {
        this.out = out;
        if (out==null) this.out = System.out;
        this.debug = debug;
        ClassLoader parent = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        this.classLoader = new NetletClassLoader(new URI[0], new URL[0], parent, null);
        this.websterStrUrl = websterStrUrl;
    }

    public ServiceScripter(File scriptFile) throws IOException {
        this(scriptFile, null, null, null);
    }

    public ServiceScripter(File scriptFile, ClassLoader classLoader) throws IOException {
        this(scriptFile, null, classLoader, null);
    }

    public ServiceScripter(File scriptFile, PrintStream out, ClassLoader classLoader, String websterStrUrl) throws IOException {
        this(out, classLoader, websterStrUrl, false);
        script = IOUtils.toString(new FileReader(scriptFile));
    }

    public ServiceScripter(String script, PrintStream out, ClassLoader classLoader, String websterStrUrl) throws IOException {
        this(out, classLoader, websterStrUrl, false);
        this.script = script;
    }

    public Object execute() throws Throwable {
        if (scripterThread != null) {
            if (target == null) {
                scripterThread.evalScript();
            }
            scripterThread.exert();
            result = scripterThread.getResult();
            return result;
        }
        throw new ScriptExertException("You must first prc compute() before calling exert() ");
    }

    public Object interpret() throws Throwable {
        try {
            if (out!=null && debug) out.println("creating scripterThread..."+ (System.currentTimeMillis()-startTime)+"ms");
            scripterThread = new ScripterThread(script, classLoader, isExertable);
            scripterThread.evalScript();
            if (out!=null && debug) out.println("getValue target..." + (System.currentTimeMillis()-startTime)+"ms");
            this.target = scripterThread.getTarget();
            if (target instanceof Routine) {
                isExertable = true;
            } else {
                isExertable = false;
            }
            this.serviceShell = scripterThread.getServiceShell();
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
                            groovyErrMsg + (lineNum ));
                }
                throw new ScriptExertException(msg, e, lineNum );
            }
            for (StackTraceElement st : e.getStackTrace()) {
                if (st.getClassName().equals("Script1")) {
                    lineNum = st.getLineNumber();
                    break;
                }
            }
            throw new ScriptExertException(e.getLocalizedMessage(), e, lineNum );
        } catch (Exception e) {
            logger.error( "error while parsing", e);
            throw new ScriptExertException(e.getLocalizedMessage(), e, 0);
        }
    }

    public void readFile(File scriptFile) throws IOException {
        this.script = IOUtils.toString(new FileReader(scriptFile));
    }

    public void readScriptWithHeaders(String script) {
        this.script = script;
    }

    public void setIsExerted(boolean isExerted) {
        this.isExertable = isExerted;
    }

    public Object getTarget() {
        return target;
    }

    public Object getResult() {
        return result;
    }

    public boolean isExertable() {
        return isExertable;
    }

    public ServiceShell getServiceShell() {
        return serviceShell;
    }

    public void setConfig(Configuration config) {
        this.config = config;
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


