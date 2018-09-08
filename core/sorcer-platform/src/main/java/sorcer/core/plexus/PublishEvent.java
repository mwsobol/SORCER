package sorcer.core.plexus;

import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEvent;

import java.rmi.MarshalledObject;
import java.util.Random;

/**
 * Created by MIke Sobolewski on 7/5/16.
 */
public class PublishEvent extends RemoteEvent {
    static final long serialVersionUID = 1L;

    protected Entry entry;

    protected Object cause;

    public
    PublishEvent(Object source, long eventID, long seqNum,
                        MarshalledObject<?> handback, int state) {
        super(source, eventID, seqNum, handback);
    }

    /**
     * <p>
     * Returns the entry of this remote event.
     * </p>
     *
     * @return the modelingContext
     */
    public Entry getEntry() {
        return entry;
    }

    /**
     * <p>
     * Assigns the entry to this remote event.
     * </p>
     *
     * @param entry
     *            the modelingContext to setValue
     * @return nothing
     */
    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    /**
     * <p>
     * Returns the kind (subtype) of this event;
     * </p>
     *
     * @return the kind
     */
    public Object getCause() {
        return cause;
    }

    /**
     * <p>
     * Assigns the cause (subtype) of this event;
     * </p>
     *
     * @param cause
     *            the cause to setValue
     * @return nothing
     */
    public void setCause(Object cause) {
        this.cause = cause;
    }


    public String toString() {
        String name = null;
        if (entry != null)
            name = entry.toString();

        return "ContextEvent=>modelingContext|source|eventID|seqNum:multitype-cause=" + name
                + source + "|" + eventID + "|" + seqNum + "|" + cause;
    }

    static public long getEventID() {
        return new Random().nextLong();
    }

}
