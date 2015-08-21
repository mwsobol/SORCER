package sorcer.ui.exertlet;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.logger.LoggerRemoteEvent;

import javax.swing.*;
import java.io.PrintStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;

/**
 * Created by Mike Sobolewski on 8/21/15.
 */


public class NetletLoggerListener implements RemoteEventListener, Serializable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NetletLoggerListener.class);

    private JTextArea pane;

    public NetletLoggerListener(JTextArea feedbackPane) {
        this.pane = feedbackPane;
        pane.setText("");
    }

    @Override
    public void notify(RemoteEvent event) throws UnknownEventException, RemoteException {
        LoggerRemoteEvent logEvent = (LoggerRemoteEvent)event;
        ILoggingEvent le = logEvent.getLoggingEvent();
        // Print everything to feedbackPane as if it was a local log
        String mogId = le.getMDCPropertyMap().get(RemoteLogger.KEY_MOGRAM_ID);
        String prvId = le.getMDCPropertyMap().get(RemoteLogger.KEY_PROVIDER_ID);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        pane.append("\n" + le.getLevel() + "  " + sdf.format(le.getTimeStamp())
                + " ["+ (mogId != null ? mogId.substring(0,8) : "NO MOGRAM ID")
                + "@" + (prvId != null ? prvId.substring(0,8) : "NO PRV ID") +"] ");
        pane.append(" " + le.getLoggerName() + " -");
        pane.append(" " + le.getFormattedMessage());
        if (le.getCallerData() != null)
            for (StackTraceElement ste : le.getCallerData()) {
                pane.append(ste.toString());
            }
    }

}
