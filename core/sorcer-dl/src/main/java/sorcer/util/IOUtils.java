package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class IOUtils {

    final private static Logger log = LoggerFactory.getLogger(IOUtils.class);

	public static void closeQuietly(ZipFile closeable) {
        if (closeable == null)
            return;
        closeQuietly(new CloseableZipFile(closeable));
	}

    protected static class CloseableZipFile implements Closeable {
        private ZipFile zipFile;

        public CloseableZipFile(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        @Override
        public void close() throws IOException {
            zipFile.close();
        }
    }

	protected static void checkFileExists(File file) throws IOException {
		if (!file.exists()) {
			throw new IOException("***error: the file does not exist: "
					+ file.getAbsolutePath());
		}
		if (!file.canRead()) {
			throw new IOException("***error: the file is not readable: "
					+ file.getAbsolutePath());
		}
	}

    // this method exits the jvm if the file or directory is not readable; the exit is
    // necessary for boot strapping providers since exceptions in provider constructors
    // are simply caught and ignored...exit brings the provider down, which is good.
    public static void checkFileExistsAndIsReadable(File file) {

        try {
            ensureFile(file, FileCheck.readable);
        } catch (IOException e) {
            System.out.println("***error: " + e.toString()
                    + "; problem with file = " + file.getAbsolutePath());
            e.printStackTrace();
            System.exit(1);
            throw new RuntimeException(e);
        }
    }

    public static enum FileCheck {
        readable("File not readable") {
            @Override
            boolean check(File file) {
                return file.canRead();
            }
        }, writable("File not writable") {
            @Override
            boolean check(File file) {
                return file.canWrite();
            }
        }, directory("Path is not a directory") {
            @Override
            boolean check(File file) {
                return file.isDirectory();
            }
        }, executable("File not executable") {
            @Override
            boolean check(File file) {
                return file.canExecute();
            }
        };

        abstract boolean check(File file);

        FileCheck(String desc) {
            description = desc;
        }

        String description;
    }

    public static void ensureFile(File file, FileCheck... checks) throws IOException {
        if (file == null)
            throw new IOException("Input file is null");
        if (!file.exists()) {
            log.debug("File {} does not exist", file.getAbsolutePath());
            throw new IOException("File " + file.getAbsolutePath() + " does not exist");
        }

        for (FileCheck check : checks) {
            if (!check.check(file)) {
                String msg = check.description + ": " + file.getAbsolutePath();
                log.error(msg);
                throw new IOException(msg);
            }
        }
    }

	/**
	 * Deletes a direcory and all its files.
	 *
	 * @param dir
	 *            to be deleted
	 * @return true if the directory is deleted
	 * @throws Exception
	 */
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}


	// this is copied from apache commons-io
	/**
	 * The default buffer size to use for
	 * {@link #copyLarge(java.io.InputStream, java.io.OutputStream)}
	 * and
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	/**
	 * Copy bytes from an <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p/>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p/>
	 * Large streams (over 2GB) will return a bytes copied value of
	 * <code>-1</code> after the copy has completed since the correct
	 * number of bytes cannot be returned as an int. For large streams
	 * use the <code>copyLarge(InputStream, OutputStream)</code> method.
	 *
	 * @param input  the <code>InputStream</code> to read from
	 * @param output the <code>OutputStream</code> to write to
	 * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
	 * @throws NullPointerException if the input or output is null
	 * @throws java.io.IOException  if an I/O error occurs
	 * @since Commons IO 1.1
	 */
	public static int copy(InputStream input, OutputStream output) throws IOException {
		long count = copyLarge(input, output);
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p/>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 *
	 * @param input  the <code>InputStream</code> to read from
	 * @param output the <code>OutputStream</code> to write to
	 * @return the number of bytes copied
	 * @throws NullPointerException if the input or output is null
	 * @throws java.io.IOException  if an I/O error occurs
	 * @since Commons IO 1.3
	 */
	public static long copyLarge(InputStream input, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	/**
	 * Unconditionally close a <code>Closeable</code>.
	 * <p/>
	 * Equivalent to {@link java.io.Closeable#close()}, except any exceptions will be ignored.
	 * This is typically used in finally blocks.
	 * <p/>
	 * Example code:
	 * <pre>
	 *   Closeable closeable = null;
	 *   try {
	 *       closeable = new FileReader("foo.txt");
	 *       // process closeable
	 *       closeable.close();
	 *   } catch (Exception e) {
	 *       // error handling
	 *   } finally {
	 *       IOUtils.closeQuietly(closeable);
	 *   }
	 * </pre>
	 *
	 * @param closeable the object to close, may be null or already closed
	 * @since Commons IO 2.0
	 */
	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

    /**
     * For compatibility with Java 6, where Socket isn't Closeable
     */
    public static void closeQuietly(Socket closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

    public static String readFileToString(String filePath) throws IOException {
        return readFileToString(new File(filePath));
    }

    public static String readFileToString(File file) throws IOException {
        StringBuilder str = new StringBuilder();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            InputStreamReader in = new InputStreamReader(fileInputStream);

            char[] buff = new char[1 << 12];
            int read;

            while ((read = in.read(buff)) > 0) {
                str.append(buff, 0, read);
            }
        } finally {
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException ignored) {
                }
        }
        return str.toString();
    }

    /**
     * Check whether a file is a child of a dir. Neither file nor dir is canonized
     */
    public static boolean isChild(File dir, File file) {
        File parent=file;
        do {
            parent = parent.getParentFile();
        } while (parent != null && !parent.equals(dir));
        return parent != null && parent.equals(dir);
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    public static List<String> readLines(File file) throws IOException {
        return readLines(file, Charset.defaultCharset());
    }

    public static List<String> readLines(File file, Charset encoding) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            return IOUtils.readLines(in, encoding);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static List<String> readLines(InputStream input, Charset encoding) throws IOException {
        InputStreamReader reader = new InputStreamReader(input, encoding);
        return readLines(reader);
    }

    public static List<String> readLines(Reader input) throws IOException {
        BufferedReader reader = toBufferedReader(input);
        List<String> list = new ArrayList<String>();
        String line = reader.readLine();
        while (line != null) {
            list.add(line);
            line = reader.readLine();
        }
        return list;
    }

    public static BufferedReader toBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }
}
