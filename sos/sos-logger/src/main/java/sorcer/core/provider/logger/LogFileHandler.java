/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sorcer.core.provider.logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class LogFileHandler extends StreamHandler {
	private MeteredStream meter;
	private File file;
	private FileOutputStream fout;

	public LogFileHandler(File logFile) throws IOException {
		this.file = logFile;
		configure();
		openFile();
	}

	public synchronized void publish(LogRecord record) {
		try {
			openFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.publish(record);
		flush();
		close();
	}

	// Private method to configure a FileHandler from LogManager
	// properties and/or default values as specified in the class
	// javadoc.

	private void configure() {
		setLevel(Level.ALL);
		setFilter(null);
		setFormatter(new SimpleFormatter());
	}

	public synchronized void openFile() throws IOException {
		int len = 0;
		len = (int) file.length();

		fout = new FileOutputStream(file, true);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		meter = new MeteredStream(bout, len);
		setOutputStream(meter);
	}

	public synchronized void close() {
		super.close();
		try {
			fout.close();
		} catch (Exception ex) {
			// Problems closing the stream. Punt.
		}
	}

	private class MeteredStream extends OutputStream {
		OutputStream out;
		int written;

		MeteredStream(OutputStream out, int written) {
			this.out = out;
			this.written = written;
		}

		public void write(int b) throws IOException {
			out.write(b);
			written++;
		}

		public void write(byte buff[]) throws IOException {
			out.write(buff);
			written += buff.length;
		}

		public void write(byte buff[], int off, int len) throws IOException {
			out.write(buff, off, len);
			written += len;
		}

		public void flush() throws IOException {
			out.flush();
		}

		public void close() throws IOException {
			out.close();
		}
	}
}