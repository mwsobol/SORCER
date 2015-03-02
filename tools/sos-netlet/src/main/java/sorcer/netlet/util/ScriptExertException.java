package sorcer.netlet.util;

/**
 * SORCER class
 * User: prubach
 * Date: 02.07.13
 */
public class ScriptExertException extends Exception {

    private static final long serialVersionUID = 1111821632555733930L;
    int lineNum;
    Throwable cause;

    public ScriptExertException(String msg) {
        super(msg);
    }


    public ScriptExertException(String msg, Throwable cause, int lineNum) {
        super(msg);
        this.lineNum = lineNum;
        this.cause = cause;
    }

    public int getLineNum() {
        return lineNum;
    }

    public Throwable getCause() {
        return cause;
    }
}
