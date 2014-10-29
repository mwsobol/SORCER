/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package sorcer.util.exec;

import java.io.InputStream;

/**
 * Input stream that is always at EOF. Similar to /dev/null. Useful when there
 * is a need to indicate "no data" while the data is expected to have the form
 * of an input stream.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class NullInputStream extends InputStream implements Input {
    /** Creates a new NullInputStream that is always at EOF. */
    public NullInputStream() {}
    /** Returns -1. */
    public int read() { return -1; }
    /** Returns -1. */
    public int read(byte[] buf) { return -1; }
    /** Returns -1. */
    public int read(byte[] buf, int off, int len) { return -1; }
    /** Returns 0. */
    public int available() { return 0; }
    /** Returns 0. */
    public long skip(long n) { return 0; }
}
