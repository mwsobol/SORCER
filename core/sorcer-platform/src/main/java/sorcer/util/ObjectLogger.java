/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.util.ArrayList;

import net.jini.io.MarshalInputStream;
import net.jini.io.MarshalOutputStream;
import net.jini.io.MarshalledInstance;

public class ObjectLogger {
	private static Class resourceClass = null;

	public static synchronized void persist(String filename, Object item)
			throws IOException {
		persist(filename, item, false);
	}

	public static synchronized void persist(String filename, Object item,
			boolean isAbsolute) throws IOException {
		ObjectOutputStream oos = null;
		try {
			if (!isAbsolute)
				oos = new ObjectOutputStream(new FileOutputStream(filename));
			else
				oos = new ObjectOutputStream(new FileOutputStream(filename));
			oos.writeObject(item);
			oos.flush();
		} finally {
			if (oos != null)
				oos.close();
		}
	}

	public static synchronized Object restore(String filename)
			throws IOException, ClassNotFoundException {
		InputStream is = null;
		ObjectInputStream ois = null;
		Object obj = null;
		try {
			if (resourceClass != null) {
				is = resourceClass.getResourceAsStream(filename);
				ois = new ObjectInputStream(is);
			} else
				ois = new ObjectInputStream(new FileInputStream(filename));
			obj = ois.readObject();
		} finally {
			if (is != null)
				is.close();
			if (ois != null)
				ois.close();
		}
		return obj;
	}

	public static synchronized void persistMarshalled(String filename,
			Object item) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new BufferedOutputStream(
					new FileOutputStream(filename)));
			oos.writeObject(new MarshalledObject(item));
		} finally {
			if (oos != null)
				oos.close();
		}
		// System.out.println("Wrote to " + logDir + filename);
	}

	public static synchronized Object restoreMarshalled(String filename)
			throws IOException, ClassNotFoundException {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(
					new FileInputStream(filename)));
			MarshalledObject o = (MarshalledObject) ois.readObject();
			// System.out.println("Read MarshalledObject from file " +
			// filename);
			return o.get();
		} finally {
			if (ois != null)
				ois.close();
		}
	}

	public static synchronized Object restoreMarshalled(InputStream input)
			throws IOException, ClassNotFoundException {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(input));
			MarshalledObject o = (MarshalledObject) ois.readObject();
			return o.get();
		} finally {
			if (ois != null)
				ois.close();
		}
	}
	
	public static synchronized void persistAnnotatedMarshalled(String filename,
			Object item) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new MarshalOutputStream(new BufferedOutputStream(
					new FileOutputStream(filename)), new ArrayList());
			oos.writeObject(new MarshalledInstance(item).get(false));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (oos != null)
				oos.close();
		}
		// System.out.println("Wrote to " + logDir + filename);
	}

	public static synchronized void persistAnnotatedMarshalled(File file,
			Object item) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new MarshalOutputStream(new BufferedOutputStream(
					new FileOutputStream(file)), new ArrayList());
			oos.writeObject(new MarshalledInstance(item).get(false));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (oos != null)
				oos.close();
		}
		// System.out.println("Wrote to " + logDir + filename);
	}
	
	public static synchronized Object restoreAnnotatedMarshalled(String filename)
			throws IOException, ClassNotFoundException {
		ObjectInputStream ois = null;
		try {
			ois = new MarshalInputStream(
					new FileInputStream(filename), null, false, null,
					new ArrayList());
			Object o = ois.readObject();
			// System.out.println("Read MarshalledObject from file " +
			// filename);
			return o;
		} finally {
			if (ois != null)
				ois.close();
		}
	}

	public static synchronized Object restore(URL url) throws IOException,
			ClassNotFoundException {
		ObjectInputStream ois = null;
		Object obj = null;
		try {
			ois = new ObjectInputStream(url.openStream());
			obj = ois.readObject();
		} finally {
			if (ois != null)
				ois.close();
		}
		return obj;
	}

	public static synchronized Object restoreMarshalled(URL url)
			throws IOException, ClassNotFoundException {
		ObjectInputStream ois = null;
		Object obj = null;
		try {
			ois = new ObjectInputStream(url.openStream());
			obj = ((MarshalledObject) ois.readObject()).get();
		} finally {
			if (ois != null)
				ois.close();
		}
		return obj;
	}

	public static void setResourceClass(Class rclass) {
		resourceClass = rclass;
	}
}
