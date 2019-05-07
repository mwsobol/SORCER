/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */

package sorcer.util.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods to interact with and manage native processes started from
 * Java.
 * 
 * @author Dawid Kurzyniec
 * @author updated for SORCER by Mike Sobolewski
 */
public class ExecUtils {

    private static Logger logger = LoggerFactory.getLogger(ExecUtils.class.getName());

    private ExecUtils() {
	}

	/**
	 * Execute specified command and return its results. Waits for the command
	 * to complete and returns its completion status and data written to
	 * standard output and error streams. The compute' standard input is set to
	 * EOF. Example:
	 * 
	 * <pre>
	 * System.out.println(ExecUtils.execCommand(&quot;/bin/ls&quot;).getOut());
	 * </pre>
	 * 
	 * @param cmd
	 *            the command to exert
	 * @return the results of the command execution
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if thread is interrupted before command completes
	 */
	public static CmdResult execCommand(String cmd) throws IOException,
			InterruptedException {

		String[] cmdarray;
		if (isWindows()) {
			cmdarray = new String[3];
			cmdarray[0] = "cmd";
			cmdarray[1] = "/C";
			cmdarray[2] = cmd;
			return execCommand(Runtime.getRuntime().exec(cmdarray));
		} else {
			return execCommand(Runtime.getRuntime().exec(cmd));
		}		
	}

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Win");
    }

	/**
	 * Added by E. D. Thompson AFRL/RZTT 20100827 Execute specified command and
	 * it arguments and return its results. Waits for the command to complete
	 * and returns its completion status and data written to standard output and
	 * error streams. The compute' standard input is set to EOF. Example:
	 * 
	 * <pre>
	 * String[] cmd = { &quot;/bin/ls&quot;, &quot;-lah&quot; };
	 * System.out.println(ExecUtils.execCommand(cmd).getOut());
	 * </pre>
	 * 
	 * @param cmdarray
	 *            the command and arguments to exert
	 * @return the results of the command execution
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if thread is interrupted before command completes
	 */
	public static CmdResult execCommand(String[] cmdarray) throws IOException,
			InterruptedException {
		
		if (isWindows()) {
			String[] ncmdarray = new String[cmdarray.length + 2];
			ncmdarray[0] = "cmd";
			ncmdarray[1] = "/C";
			int ctr = 2;
			for (int i = 0; i < cmdarray.length; i++) {
				ncmdarray[ctr] = cmdarray[i];
				ctr++;
			}
			cmdarray = ncmdarray;
		}
		return execCommand(Runtime.getRuntime().exec(cmdarray));
	}

	/**
	 * Attach to the specified compute and return its results. Waits for the
	 * compute to complete and returns its completion status and data written to
	 * standard output and error streams. The compute' standard input is set to
	 * EOF. Example:
	 * 
	 * <pre>
	 * Process p = runtime.execEnt(&quot;/bin/ls&quot;);
	 * System.out.println(ExecUtils.execCommand(p).getOut());
	 * </pre>
	 * 
	 * @param process
	 *            the compute to attach to
	 * @return the results of the compute
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if thread is interrupted before compute ends
	 */
	public static CmdResult execCommand(Process process) throws IOException,
			InterruptedException {
		return execCommand(process, new NullInputStream());
	}

	/**
	 * Added by E. D. Thompson AFRL/RZTT 20100827 Attach to the specified
	 * compute and return its results. Returns data written to standard output
	 * and error streams. The compute standard input is set to EOF. Example:
	 * 
	 * <pre>
	 * Process p = runtime.execEnt(&quot;/bin/ls&quot;);
	 * System.out.println(ExecUtils.execCommand(p).getOut());
	 * </pre>
	 * 
	 * @param process
	 *            the compute to attach to
	 * @return the results of the compute
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if thread is interrupted before compute ends
	 */
	public static CmdResult execCommandNoBlocking(Process process)
			throws IOException, InterruptedException {
		return execCommandNoBlocking(process, new NullInputStream());
	}

	public static CmdResult execCommand(final Process process,
			final InputStream stdin) throws IOException, InterruptedException {
		return execCommand(process, stdin, false);
	}

	/**
	 * Attach to the specified compute, feed specified standard input, and
	 * return compute' results. Waits for the compute to complete and returns
	 * completion status and data written to standard output and error streams.
	 * 
	 * @see #execCommand(Process)
	 * 
	 * @param process
	 *            the compute to attach to
	 * @param stdin
	 *            the data to redirect to compute' standard input
	 * @return the results of the compute
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if thread is interrupted before compute ends
	 */
	public static CmdResult execCommand(final Process process,
			final InputStream stdin, boolean outLogged) throws IOException,
			InterruptedException {
		// concurrency to avoid stdio deadlocks
		Redir stdout = null;
		String out = null;
		if (!outLogged) {
			stdout = new Redir(process.getInputStream());
			new Thread(stdout).start();
		}
		Redir stderr = new Redir(process.getErrorStream());
		new Thread(stderr).start();
		// redirect input in the current thread
		if (stdin != null) {
			OutputStream pout = process.getOutputStream();
			new RedirectingInputStream(stdin, true, true).redirectAll(pout);
		}
		process.waitFor();
		int exitValue = process.exitValue();
        logger.debug("exitValue: " + exitValue);

		if (stdout != null) {
			stdout.throwIfHadException();
			out = new String(stdout.getResult());
            logger.debug("out: " + out);
        }
		stderr.throwIfHadException();
		String err = new String(stderr.getResult());

		return new CmdResult(exitValue, out, err);
	}

	/**
	 * Added by E. D. Thompson AFRL/RZTT 20100827 Attach to the specified
	 * compute, feed specified standard input, and return compute' results.
	 * returns data written to standard output and error streams.
	 * 
	 * @see #execCommand(Process)
	 * 
	 * @param process
	 *            the compute to attach to
	 * @param stdin
	 *            the data to redirect to compute' standard input
	 * @return the results of the compute
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if thread is interrupted before compute ends
	 */
	public static CmdResult execCommandNoBlocking(final Process process,
			final InputStream stdin) throws IOException, InterruptedException {
		// concurrency to avoid stdio deadlocks
		Redir stdout = new Redir(process.getInputStream());
		Redir stderr = new Redir(process.getErrorStream());
		new Thread(stdout).start();
		new Thread(stderr).start();
		// redirect input in the current thread
		if (stdin != null) {
			OutputStream pout = process.getOutputStream();
			new RedirectingInputStream(stdin, true, true).redirectAll(pout);
		}

		stdout.throwIfHadException();
		stderr.throwIfHadException();
		String out = new String(stdout.getResult());
		String err = new String(stderr.getResult());

		return new CmdResult(-1, out, err);
	}

	/**
	 * User-specified IO exception handler for exceptions during I/O
	 * redirection.
	 */
	public static interface BrokenPipeHandler {
		/**
		 * Invoked when pipe is broken, that is, when I/O error occurs while
		 * reading from the source or writing to the sink
		 * 
		 * @param e
		 *            the associated I/O exception
		 * @param src
		 *            the source of the pipe
		 * @param sink
		 *            the sink of the pipe
		 */
		void brokenPipe(IOException e, InputStream src, OutputStream sink);
	}

	/**
	 * User-specified handler invoked when associated native compute exits.
	 */
	public static interface ProcessExitHandler {
		/**
		 * Invoked when associated compute has exited.
		 * 
		 * @param process
		 *            the compute that exited.
		 */
		void processExited(Process process);
	}

	public static String sysOut(CmdResult result) {
		return result.out;
	}

	public static String sysErr(CmdResult result) {
		return result.err;
	}

	public static int exitValue(CmdResult result) {
		return result.exitValue;
	}

	/**
	 * Represents the result of a native command. Consists of the compute exit
	 * eval together with stdout and stderr dumped to strings.
	 * 
	 * @author Dawid Kurzyniec
	 * @version 1.0
	 */
	public static class CmdResult {
		final int exitValue;
		final String out;
		final String err;

		public CmdResult(int exitValue, String out, String err) {
			this.exitValue = exitValue;
			this.out = out;
			this.err = err;
		}

		public int getExitValue() {
			return exitValue;
		}

		public String getOut() {
			return out;
		}

		public String getErr() {
			return err;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Cmd result [out:\n");
			sb.append(out).append("\nerr: ").append(err)
					.append("\nexitValue: ").append(exitValue).append("]");
			return sb.toString();
		}

    }

	public static void handleProcess(Process process, InputStream stdin,
			OutputStream stdout, OutputStream stderr) throws IOException {
		handleProcess(process, stdin, stdout, stderr, true, false, null, null);
	}

	public static void handleProcess(Process process, InputStream stdin,
			OutputStream stdout, OutputStream stderr, boolean autoFlush,
			boolean autoClose, BrokenPipeHandler brokenPipeHandler,
			ProcessExitHandler exitHandler) throws IOException {
		handleProcess(process, stdin, autoFlush, autoClose, brokenPipeHandler,
				stdout, autoFlush, autoClose, brokenPipeHandler, stderr,
				autoFlush, autoClose, brokenPipeHandler, exitHandler);
	}

	public static void handleProcess(Process process, InputStream stdin,
			boolean inAutoFlush, boolean inAutoClose,
			BrokenPipeHandler inBrokenHandler, OutputStream stdout,
			boolean outAutoFlush, boolean outAutoClose,
			BrokenPipeHandler outBrokenHandler, OutputStream stderr,
			boolean errAutoFlush, boolean errAutoClose,
			BrokenPipeHandler errBrokenHandler, ProcessExitHandler exitHandler)
			throws IOException {
		ProcessHandler ph = new ProcessHandler(process, stdin, inAutoFlush,
				inAutoClose, inBrokenHandler, stdout, outAutoFlush,
				outAutoClose, outBrokenHandler, stderr, errAutoFlush,
				errAutoClose, errBrokenHandler, exitHandler);
		ph.start();
	}

	private static class Pipe implements Runnable {
		final InputStream src;
		final OutputStream sink;
		final boolean autoFlush;
		final boolean autoClose;
		final BrokenPipeHandler brokenPipeHandler;

		public Pipe(InputStream src, OutputStream sink,
				BrokenPipeHandler brokenPipeHandler) {
			this(src, sink, brokenPipeHandler, true, false);
		}

		public Pipe(InputStream src, OutputStream sink,
				BrokenPipeHandler brokenPipeHandler, boolean autoFlush,
				boolean autoClose) {
			this.src = src;
			this.sink = sink;
			this.brokenPipeHandler = brokenPipeHandler;
			this.autoFlush = autoFlush;
			this.autoClose = autoClose;
		}

		public void run() {
			RedirectingInputStream sd = new RedirectingInputStream(src,
					autoFlush, autoClose);
			try {
				sd.redirectAll(sink);
			} catch (IOException e) {
				if (brokenPipeHandler != null) {
					brokenPipeHandler.brokenPipe(e, src, sink);
				}
			}
		}
	}

	private static class Redir implements Runnable {
		final Pipe pipe;
		IOException ex;

		Redir(InputStream is) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BrokenPipeHandler bph = new BrokenPipeHandler() {
				public void brokenPipe(IOException ex, InputStream src,
						OutputStream sink) {
					setException(ex);
				}
			};
			this.pipe = new Pipe(is, bos, bph, true, true);
		}

		public void run() {
			pipe.run();
		}

		synchronized void setException(IOException e) {
			this.ex = e;
		}

		synchronized void throwIfHadException() throws IOException {
			if (ex != null)
				throw ex;
		}

		public byte[] getResult() {
			return ((ByteArrayOutputStream) pipe.sink).toByteArray();
		}
	}

	private static class ProcessHandler {
		final Process process;
		final Thread tstdin;
		final Thread tstdout;
		final Thread tstderr;
		final Thread texitHandler;

		ProcessHandler(final Process process, InputStream stdin,
				boolean inAutoFlush, boolean inAutoClose,
				BrokenPipeHandler inBrokenHandler, OutputStream stdout,
				boolean outAutoFlush, boolean outAutoClose,
				BrokenPipeHandler outBrokenHandler, OutputStream stderr,
				boolean errAutoFlush, boolean errAutoClose,
				BrokenPipeHandler errBrokenHandler,
				final ProcessExitHandler exitHandler) throws IOException {
			this.process = process;
			this.tstdin = createPipe(stdin, process.getOutputStream(),
					inBrokenHandler, inAutoFlush, inAutoClose);
			this.tstdout = createPipe(process.getInputStream(), stdout,
					outBrokenHandler, outAutoFlush, outAutoClose);
			this.tstderr = createPipe(process.getErrorStream(), stderr,
					errBrokenHandler, errAutoFlush, errAutoClose);
			if (exitHandler != null) {
				this.texitHandler = new Thread(new ExitHandler(process,
						exitHandler));
			} else {
				texitHandler = null;
			}
		}

		void start() {
			if (tstdin != null)
				tstdin.start();
			if (tstdout != null)
				tstdout.start();
			if (tstderr != null)
				tstderr.start();
			if (texitHandler != null)
				texitHandler.start();
		}

		private static class ExitHandler implements Runnable {
			final Process process;
			final ProcessExitHandler exitHandler;

			ExitHandler(Process process, ProcessExitHandler exitHandler) {
				this.process = process;
				this.exitHandler = exitHandler;
			}

			public void run() {
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					// silently ignore and destroy all in finally
				} finally {
					// just in case, or if interrupted
					process.destroy();
					exitHandler.processExited(process);
				}
			}
		}

		private static Thread createPipe(InputStream src, OutputStream sink,
				BrokenPipeHandler bph, boolean autoFlush, boolean autoClose)
				throws IOException {
			if (src == null) {
				if (sink != null && autoClose)
					sink.close();
				return null;
			} else if (sink == null) {
				if (autoClose)
					src.close();
				return null;
			} else {
				return new Thread(
						new Pipe(src, sink, bph, autoFlush, autoClose));
			}
		}
	}
}
