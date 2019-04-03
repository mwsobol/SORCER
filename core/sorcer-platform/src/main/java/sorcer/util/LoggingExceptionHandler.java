package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs uncaught exceptions to SLF4J using a logger for a class that is on the bottom of exception's stack trace.
 *
 * @author Rafał Krupiński
 */
public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] stack = e.getStackTrace();
        StackTraceElement stackElem;
        if (stack.length >= 2)
            stackElem = stack[stack.length - 2];
        else
            stackElem = stack[stack.length - 1];
        Logger logger = LoggerFactory.getLogger(stackElem.getClassName());
        logger.error("Uncaught exception", e);
    }
}
