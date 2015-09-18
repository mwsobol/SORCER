package sorcer.core.provider.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.rioproject.event.RemoteServiceEvent;

import java.io.Serializable;

/**
 * SORCER class
 * User: prubach
 * Date: 05.06.14
 */
public class LoggerRemoteEvent extends RemoteServiceEvent implements Serializable {

    private static final long serialVersionUID = 95363637637727L;
    public static final long ID = 168572317278L;

    //private Vector providersList;
    private ILoggingEvent loggingEvent;

    public LoggerRemoteEvent(Object source) {
        super(source);
    }

    /** Creates new RenderEvent */
    public LoggerRemoteEvent(Object source, ILoggingEvent loggingEvent) {
        super(source);
        this.loggingEvent= loggingEvent;
    }

    public ILoggingEvent getLoggingEvent() {
        return loggingEvent;
    }
}
