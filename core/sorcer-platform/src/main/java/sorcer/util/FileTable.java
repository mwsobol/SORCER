/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.Tuple2;
import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Identity;
import sorcer.service.Signature;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class FileTable<K,V> extends Identity implements Runnable, ModelTable {

	private final static Logger logger = LoggerFactory.getLogger(FileTable.class);

	// Object File
	ObjectFile ofl;
	// Index File
	ObjectFile ifl;

	String fileName;

	private ConcurrentHashMap<K, Long> table;

	private static int WAITING_TIME = 30 * 60 * 1000; //30 min

	volatile boolean running = true;

	protected K lastKey;

	protected String inputFileName;

	protected URL inputTableURL;

	protected String inputTableDelimiter;

	protected String outputFileName;

	protected URL outputTableURL;

	protected String outputTableDelimiter;

	protected Signature outputStorageSignature;

	protected boolean includeHeaderInWrite;

	protected List<String> columnIdentifiers;

	protected List<Object> rowIdentifiers;

	protected boolean lazy;

	protected DataTable.Cell cellType;

	protected static int count = 0;

	public FileTable(String parent, String child) throws IOException {
		if (child == null) {
			throw new NullPointerException();
		}
		String tableName = null;
		if (parent != null) {
			tableName = parent + File.separator + child;
		} else {
			tableName = child;
		}
		name = child;
		this.fileName = tableName;
		ofl = new ObjectFile(fileName +".obf");
		ifl = new ObjectFile(fileName +"-index.obf");

		try { table = (ConcurrentHashMap)ifl.readObject(0); } catch (Exception e) { }

		if (table == null) table = new ConcurrentHashMap();

		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}

	public FileTable(String fileName) throws IOException {
		this(null, fileName);
	}

	public synchronized final void close() throws  IOException {
		running = false;
		ofl.close();
		ifl.close();
	}

	public synchronized final void put(K key, V value) throws IOException {
		if (! (value instanceof Serializable))
			throw new IOException("Not serializable eval");
		Long oldPos = table.get(key);
		long newPos;
		if (oldPos == null)
			newPos = ofl.writeObject((Serializable)value);
		else
			newPos = ofl.writeObject((Serializable)value, oldPos.longValue());

		lastKey = key;
		table.put(key, newPos);
		ifl.rewriteObject(0, table);
	}

	public void addRow(K index, V row) throws IOException {
		put(index, row);
	}

	public V getRow(K key) throws IOException {
		return get(key);
	}

	public final V get(K key) throws IOException {
		Long pos = table.get(key);
		if (pos == null) return null;
		else return (V)ofl.readObject(pos.longValue());
	}

	public Set<Map.Entry<K,Long>> entrySet() {
		return table.entrySet();
	}

	public Set<K> keySet() {
		return table.keySet();
	}

	public Enumeration keys() {
		return table.keys();
	}

	public Collection<Long> indexes() {
		return table.values();
	}

	public boolean containsKey(Object key) {
		return table.containsKey(key);
	}


	public synchronized final void remove(K key) throws IOException {
		table.remove(key);
		ifl.rewriteObject(0, table);
	}

	public synchronized void cleanup() throws IOException {
		ObjectFile tmp = new ObjectFile(fileName + "-temp.obf");
		ConcurrentHashMap newTable = new ConcurrentHashMap();

		K key;
		Enumeration<K>  e = table.keys();
		while (e.hasMoreElements()) {
			key = e.nextElement();
			newTable.put(key , new Long(tmp.writeObject((Serializable)get(key))));
		}

		table = newTable;
		ifl.rewriteObject(0, table);

		ofl.close();
		tmp.close();

		new File(fileName+"-temp.obf").renameTo(new File(fileName + ".obf"));

		ofl = new ObjectFile(fileName + ".obf");
	}

	public void run() {
		while (running) {
			try {
				Thread.sleep(WAITING_TIME);
				cleanup();
			}catch (Exception e) { e.printStackTrace(); }
		}
	}

	public static class ObjectFile {

        RandomAccessFile dataFile;

		public ObjectFile(String fileName) throws IOException {
			dataFile = new RandomAccessFile(fileName, "rw");
		}

		public byte[] getBytes(Serializable obj) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(new MarshalledObject(obj));
			oos.flush();

			byte[] b = baos.toByteArray();
			baos = null; oos = null;

			return b;
		}

		// returns file position object was written to.
		public synchronized long writeObject(Serializable obj) throws IOException {
			//write at end
			return writeObject(getBytes(obj), dataFile.length());
		}


		// returns file position object was written to.
		public synchronized long writeObject(Serializable obj, long lPos) throws IOException {
			dataFile.seek(lPos);
			int oldlen = dataFile.readInt();

			byte[] b = getBytes(obj);
			int newlen = b.length;

			return (oldlen >= newlen)?
					writeObject(b, lPos) : writeObject(b, dataFile.length());
		}

		private synchronized long writeObject(byte[] b, long pos) throws IOException {
			int datalen = b.length;

			dataFile.seek(pos);

			// write the length of the output
			dataFile.writeInt(datalen);
			dataFile.write(b);

			return pos;
		}

		public synchronized Object readObject(long lPos)
				throws IOException {
			dataFile.seek(lPos);
			int datalen = dataFile.readInt();
			if (datalen > dataFile.length())
				throw new IOException("Data file is corrupted, length: "
						+ datalen);
			byte [] data = new byte[datalen];
			dataFile.readFully(data);

			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);
			MarshalledObject o ;

			try {
				o = (MarshalledObject)ois.readObject();
				bais = null; ois = null; data = null;

				return o.get();
			} catch (ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
				throw new IOException("Class Not found:" + cnfe.getMessage());
			}
		}

		public synchronized void rewriteObject(long pos,
											   Serializable obj) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(new MarshalledObject(obj));
			oos.flush();

			int datalen = baos.size();

			// insert record
			dataFile.seek(pos);

			// write the length of the output
			dataFile.writeInt(datalen);
			dataFile.write(baos.toByteArray());

			baos.close();
			oos.close();
		}

		public void close() throws IOException {
			dataFile.close();
		}

        public RandomAccessFile getDataFile() {
            return dataFile;
        }
	}

	/**
	 * <p>
	 * Returns the output file for this dataTable.
	 * </p>
	 *
	 * @return the inputFile
	 */
	public String getInputFile() {
		return inputFileName;
	}

	/**
	 * <p>
	 * Sets the output file for this dataTable.
	 * </p>
	 *
	 * @param inputFile
	 *            the inputFile to setValue
	 */
	public void setInputFile(String inputFile) {
		this.inputFileName = inputFile;
	}

	/**
	 * <p>
	 * Returns the output file for this dataTable.
	 * </p>
	 *
	 * @return the outputFile
	 */
	public String getOutputFile() {
		return outputFileName;
	}

	/**
	 * <p>
	 * Sets the output file for this dataTable.
	 * </p>
	 *
	 * @param outputFile
	 *            the outputFile to setValue
	 */
	public void setOutputFile(String outputFile) {
		this.outputFileName = outputFile;
	}

	/**
	 * <p>
	 * Sets the output URL for this dataTable.
	 * </p>
	 *
	 * @return the outputURL
	 */
	public URL getOutputURL() {
		return outputTableURL;
	}

	/**
	 * <p>
	 * Sets the output URL for this dataTable.
	 * </p>
	 *
	 * @param outputURL
	 *            the outputURL to setValue
	 */
	public void setOutputURL(URL outputURL) {
		this.outputTableURL = outputURL;
	}

	/**
	 * <p>
	 * Sets the input URL for this dataTable.
	 * </p>
	 *
	 * @return the inputURL
	 */
	public URL getInputURL() {
		return inputTableURL;
	}

	/**
	 * <p>
	 * Sets the input URL for this dataTable.
	 * </p>
	 *
	 * @param inputURL
	 *            the inputURL to setValue
	 */
	public void setInputURL(URL inputURL) {
		this.inputTableURL = inputURL;
	}

	public void setOutput(String location) throws MalformedURLException {
		if (location != null) {
			if (location.startsWith("http://") || location.startsWith("sos://"))
				outputTableURL = new URL(location);
			else
				outputFileName = location;
		}
	}

	public void setInput(String location) throws MalformedURLException {
		setInput(location, null);
	}

	public void setInput(String location, String inDelimiter) throws MalformedURLException {
		if (location != null) {
			if (location.startsWith("http://") || location.startsWith("sos://"))
				inputTableURL = new URL(location);
			else
				inputFileName = location;
		}
		if (inDelimiter != null)
			this.inputTableDelimiter = inDelimiter;
	}

	/**
	 * <p>
	 * Returns the input delimiter for this dataTable.
	 * </p>
	 *
	 * @return the inDelimiter
	 */
	public String getInDelimiter() {
		return inputTableDelimiter;
	}

	/**
	 * <p>
	 * Sets the input delimiter for this dataTable.
	 * </p>
	 *
	 * @param inDelimiter
	 *            the inDelimiter to setValue
	 */
	public void setInDelimiter(String inDelimiter) {
		this.inputTableDelimiter = inDelimiter;
	}

	/**
	 * <p>
	 * Returns the output delimiter for this dataTable.
	 * </p>
	 *
	 * @return the outDelimiter
	 */
	public String getOutDelimiter() {
		return outputTableDelimiter;
	}

	/**
	 * <p>
	 * Sets the output delimiter for this dataTable.
	 * </p>
	 *
	 * @param outDelimiter
	 *            the outDelimiter to setValue
	 */
	public void setOutDelimiter(String outDelimiter) {
		this.outputTableDelimiter = outDelimiter;
	}

	public List<String> getColumnIdentifiers(){
		return columnIdentifiers;
	}

	public void setRowIdentifier(String rowIdentifier, int index) {
		if (index >= rowIdentifiers.size()) rowIdentifiers.add(rowIdentifier);
		else rowIdentifiers.add(index, rowIdentifier);
	}

	public List<Object> getRowIdentifiers(){
		return rowIdentifiers;
	}

	/**
	 * Replaces the column identifiers in the model. If the number of
	 * <code>newIdentifier</code>s is greater than the current number of
	 * columns, new columns are added to the end of each row in the model. If
	 * the number of <code>newIdentifier</code>s is less than the current number
	 * of columns, all the extra columns at the end of a row are discarded.
	 * <p>
	 *
	 * @param columnIdentifiers
	 *            list of column identifiers. If <code>null</code>, setValue the
	 *            model to zero columns
	 */
	public void setColumnIdentifiers(List columnIdentifiers) {
		this.columnIdentifiers = nonNullList(columnIdentifiers);
	}

	protected static List nonNullList(List l) {
		return (l != null) ? l : newList();
	}

	private static List<?> newList() {
		List<?> l = new ArrayList<Object>();
		return Collections.synchronizedList(l);
	}

	public void setRowIdentifiers(List rowIdentifiers) {
		this.rowIdentifiers = rowIdentifiers;
	}

	/**
	 * Replaces the column identifiers in the model. If the number of
	 * <code>newIdentifier</code>s is greater than the current number of
	 * columns, new columns are added to the end of each row in the model. If
	 * the number of <code>newIdentifier</code>s is less than the current number
	 * of columns, all the extra columns at the end of a row are discarded.
	 * <p>
	 *
	 * @param newIdentifiers
	 *            array of column identifiers. If <code>null</code>, setValue the
	 *            model to zero columns
	 */
	public void setColumnIdentifiers(Object[] newIdentifiers) {
		setColumnIdentifiers(convertToList(newIdentifiers));
	}


	/**
	 * Returns a list that contains the same objects as the array.
	 *
	 * @param anArray
	 *            the array to be converted
	 * @return the new list; if <code>anArray</code> is <code>null</code>,
	 *         returns <code>null</code>
	 */
	protected static List convertToList(Object[] anArray) {
		if (anArray == null) {
			return null;
		}
		List l = new ArrayList(anArray.length);
		for (int i = 0; i < anArray.length; i++) {
			l.add(anArray[i]);
		}
		return l;
	}

	/**
	 * Returns the key of last recorded entry in this data dataTable.
	 *
	 * @return the last Entry
	 */
	public K getLastKey() {
		return lastKey;
	}


	/**
	 * Returns the last recorded eval in this data dataTable.
	 *
	 * @return the last dataTable entry
	 */
	public V getLastValue() throws IOException {
		return get(lastKey);
	}

	public Tuple2<K, V> getLastEntry() throws IOException {
		if (lastKey != null)
			return new Tuple2(lastKey, get(lastKey));
		else
			return null;
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * Returns the number of rows in this data dataTable.
	 *
	 * @return the number of rows in the model
	 */
	public int getRowCount() {
		if (table == null)
			return 0;
		else
			return table.size();
	}

	public ConcurrentHashMap<K, Long> getTable() {
		return table;
	}

	public Context getFileContext() throws ContextException {
		ServiceContext sc = new ServiceContext(this.getName());

        sc.putValue("object/file/key", fileName +".obf");
        sc.putValue("index/file/key", fileName +"-index.obf");

		sc.putValue("input/file/key", inputFileName);
		sc.putValue("input/table/URL", inputTableURL);

		sc.putValue("output/file/key", outputFileName);
		sc.putValue("output/table/URL", outputTableURL);
		sc.putValue("input/table/delimiter", inputTableDelimiter);

		return sc;
	}

	public void delete() {
		File obf = new File(fileName +".obf");
		File iobf = new File(fileName +"-index.obf");
		obf.delete();
		iobf.delete();
	}

	@Override
	public String toString() {
		try {
			return getClass() + ":" + new File(fileName).getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			return getClass() + ":" + fileName;
		}
	}
}

