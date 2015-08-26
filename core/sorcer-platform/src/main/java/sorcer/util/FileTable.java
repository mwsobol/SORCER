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

import java.io.*;
import java.util.*;
import net.jini.id.Uuid;
import java.rmi.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileTable<K,V> implements Runnable {

	// Object File
	ObjectFile ofl;
	// Index File
	ObjectFile ifl;

	String fileName;

	private ConcurrentHashMap<K, Long> table;

	private static int WAITING_TIME = 15 * 60 * 1000; //15 min

	boolean running = true;

	public FileTable(String fileName) throws IOException {

		this.fileName = fileName;

		ofl = new ObjectFile(fileName +".obf");
		ifl = new ObjectFile(fileName +"-index.obf");

		try { table = (ConcurrentHashMap)ifl.readObject(0); } catch (Exception e) { }

		if (table == null) table = new ConcurrentHashMap();

		Thread t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}

	public synchronized final void close() throws  IOException {
		running = false;
		ofl.close();
		ifl.close();
	}

	public synchronized final void put(K key, V o) throws IOException {
		if (! (o instanceof Serializable))
			throw new IOException("Not serializable value");
		Long oldPos = (Long)table.get(key);
		long newPos;
		if (oldPos == null)
			newPos = ofl.writeObject((Serializable)o);
		else
			newPos = ofl.writeObject((Serializable)o, oldPos.longValue());

		table.put(key, new Long(newPos));
		ifl.rewriteObject(0, table);
	}

	public void addRow(K index, V row) throws IOException {
		put(index, row);
	}

	public V getRow(K key) throws IOException {
		return get(key);
	}

	public synchronized final V get(K key) throws IOException {
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
				throw new IOException("Data file is corrupted. Data length: "
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
				throw new IOException("Class Not found Exception msg:" + cnfe.getMessage());
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
	}

}

