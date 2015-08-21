package sorcer.util;

        import ch.qos.logback.classic.spi.ILoggingEvent;
        import net.jini.core.event.RemoteEvent;
        import net.jini.core.event.RemoteEventListener;
        import net.jini.core.event.UnknownEventException;
        import org.slf4j.LoggerFactory;
        import sorcer.core.provider.RemoteLogger;
        import sorcer.core.provider.logger.LoggerRemoteEvent;

        import java.io.PrintStream;
        import java.io.Serializable;
        import java.rmi.RemoteException;
        import java.text.SimpleDateFormat;

/**
 * SORCER class
 * User: prubach
 * Date: 09.06.14
 */
public class ConsoleLoggerListener implements RemoteEventListener, Serializable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ConsoleLoggerListener.class);

    private PrintStream out;

    public ConsoleLoggerListener(PrintStream out) {
        this.out = out;
    }

    @Override
    public void notify(RemoteEvent event) throws UnknownEventException, RemoteException {
        LoggerRemoteEvent logEvent = (LoggerRemoteEvent)event;
        ILoggingEvent le = logEvent.getLoggingEvent();
        // Print everything to console as if it was a local log
        String exId = le.getMDCPropertyMap().get(RemoteLogger.KEY_MOGRAM_ID);
        String prvId = le.getMDCPropertyMap().get(RemoteLogger.KEY_PROVIDER_ID);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        out.print(le.getLevel() + "  " + sdf.format(le.getTimeStamp()) +
                " ["+ (exId!=null ? exId.substring(0,8) : "NO MOGRAM ID") + "@" + (prvId!=null ? prvId.substring(0,8) : "NO PRV ID") +"] ");
        out.print(" " + le.getLoggerName() + " -" );
        out.println(" " + le.getFormattedMessage());
        if (le.getCallerData()!=null)
            for (StackTraceElement ste : le.getCallerData()) {
                out.println(ste.toString());
            }
        //logger.info(exId);
        //((ch.qos.logback.classic.Logger)logger).callAppenders(logEvent.getLoggingEvent());
    }
}
