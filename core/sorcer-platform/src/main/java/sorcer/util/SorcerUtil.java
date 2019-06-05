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

import sorcer.core.SorcerConstants;
import sorcer.service.Identifiable;

import java.io.*;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SorcerUtil implements SorcerConstants {
	final static Logger logger = LoggerFactory.getLogger("sorcer");
	private static Calendar calendar = null;

	public static String[] pathToArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, SorcerConstants.CPS);
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}

	public static String toPath(String[] array) {
		if (array.length > 0) {
			StringBuilder sb = new StringBuilder(array[0]);
			for (int i = 1; i < array.length; i++) {
				sb.append(SorcerConstants.CPS).append(array[i]);
			}
			return sb.toString();
		} else
			return null;
	}

	/**
	 * Returns a string representation of recursive arrays of any component
	 * multitype. in the form [e1,...,ek]
	 */
	public static String arrayToString(Object array) {
		if (array == null)
			return "null";
		else if (!array.getClass().isArray()) {
			return array.toString();
		}
		int length = Array.getLength(array);
		if (length == 0)
			return "[no elements]";

		StringBuffer buffer = new StringBuffer("[");
		int last = length - 1;
		Object obj;
		for (int i = 0; i < length; i++) {
			obj = Array.get(array, i);
			if (obj == null)
				buffer.append("null");
			else if (obj.getClass().isArray())
				buffer.append(arrayToString(obj));
			else
				buffer.append(obj);

			if (i == last)
				buffer.append("]");
			else
				buffer.append(",");
		}
		return buffer.toString();
	}

	public static List<String> getNames(List<? extends Identifiable> list) {
		List<String> names = new ArrayList<String>(list.size());
		for (Identifiable i : list) {
			names.add(i.getName());
		}
		return names;
	}
	
	/**
	 * Break string into an array of tokens. The delimiter is passed to
	 * StringTokenizer for tokenizing the string.
	 * 
	 * @param str
	 *            string to break up.
	 * @param delim
	 *            delimiter string.
	 * @return token array.
	 */
	public static String[] getTokens(String str, String delim) {
		Vector<String> tokens = new Vector<String>();
		StringTokenizer tokenizer = new StringTokenizer(str, delim);
		while (tokenizer.hasMoreTokens()) {
			tokens.addElement(tokenizer.nextToken());
		}
		String[] returnTokens = new String[tokens.size()];
		tokens.copyInto(returnTokens);
		return returnTokens;
	}

	/**
	 * Break string into an array of CVS tokens. The delimiter is passed to
	 * StringTokenizer for tokenizing the string.
	 * 
	 * @param str
	 *            string to break up.
	 * @param delim
	 *            delimiter string.
	 * @return token array.
	 */
	public static String[] tokenize(String str, String delim) {
		Vector<String> tokens = new Vector<String>();
		String token = "";

		try {
			CSVStringTokenizer tokenizer = new CSVStringTokenizer(str, delim);
			while (tokenizer.hasMoreTokens()) {
				tokens.addElement((token = tokenizer.nextToken())
						.equals("null") ? "" : token);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] returnTokens = new String[tokens.size()];
		tokens.copyInto(returnTokens);
		return returnTokens;
	}

	public static String[] firstTwoTokens(String str, String delim) {
		String out[] = new String[2];

		try {
			CSVStringTokenizer token = new CSVStringTokenizer(str, delim);
			out[0] = token.nextToken();
			out[1] = token.nextToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static String firstToken(String str, String delim) {
		String out = new String();

		try {
			CSVStringTokenizer token = new CSVStringTokenizer(str, delim);
			out = token.nextToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static String secondToken(String str, String delim) {
		String out = new String();

		try {
			CSVStringTokenizer token = new CSVStringTokenizer(str, delim);
			token.nextToken();
			out = token.nextToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static String thirdToken(String str, String delim) {
		String out = new String();

		try {
			CSVStringTokenizer token = new CSVStringTokenizer(str, delim);
			token.nextToken();
			token.nextToken();
			out = token.nextToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static void bubbleSort(List list) {
		int i = list.size();
		while (--i >= 0) {
			for (int j = 0; j < i; j++) {
				if (((String) list.get(j)).compareTo((String) list
						.get(j + 1)) > 0) {
					/* swap objects */
					Object temp = list.get(j);
					list.set(j, list.get(j + 1));
					list.set(j + 1, temp);
				}
			}
		}
	}

	/**
	 * Replaces newline characters in the passed text with "\\n"
	 * 
	 * @param origString
	 *            The text to replace return characters from
	 */
	public static String escapeReturns(String origString) {
		StringBuffer sb = new StringBuffer();
		int len = origString.length();
		for (int i = 0; i < len; i++) {
			char c = origString.charAt(i);
			if (c == '\n')
				sb.append("\\n");
			else
				sb.append(c);
		}
		return sb.toString();
	}

	public static String urlEncode(String origString) {
		StringBuffer sb = new StringBuffer();
		int len = origString.length();
		for (int i = 0; i < len; i++) {
			char c = origString.charAt(i);
			if (c == ' ')
				sb.append("%20");
			else
				sb.append(c);
		}
		return sb.toString();
	}

	public static String detab(String s) {
		if (s.indexOf('\t') == -1)
			return s;
		StringBuffer sb = new StringBuffer();
		int len = s.length();
		int pos = 0;
		int i = 0;
		for (; i < len && s.charAt(i) == '\t'; i++) {
			sb.append("        ");
			pos += 8;
		}
		for (; i < len; i++) {
			char c = s.charAt(i);
			if (c == '\t') {
				do {
					sb.append(' ');
					pos++;
				} while (pos % 8 != 0);
			} else {
				sb.append(c);
				pos++;
			}
		}
		return sb.toString();
	}

	public static URL getURL(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException ue) {
			url = null;
		}
		return url;
	}

	/**
	 * Returns a date in a format mm/dd/yyyy format for a date
	 */
	public static String getDate(Date date) {
		if (calendar == null) {
			calendar = Calendar.getInstance();
			calendar.setTime(date);
		} else
			calendar.setTime(date);

		return getDate();
	}

	/**
	 * Returns a date in a format mm/dd/yyyy format from milliseconds
	 */
	public static String getDate(long millis) {
		if (calendar == null) {
			calendar = Calendar.getInstance();
			calendar.setTime(new Date(millis));
		} else
			calendar.setTime(new Date(millis));

		return getDate();
	}

	/**
	 * Returns a date in the format "yyyyMMdd-HHmmss" format using this class calendar
	 */
	public static String getDateTime() {
		if (calendar == null) {
			calendar = Calendar.getInstance();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		long time = calendar.getTime().getTime();
		return sdf.format(time);
	}
	
	/**
	 * Returns a date in a format mm/dd/yyyy format using this class calendar
	 */
	public static String getDate() {
		if (calendar == null) {
			calendar = Calendar.getInstance();
		}
		String dd = Integer.toString(calendar.get(Calendar.DATE));
		if (dd.length() == 1)
			dd = "0" + dd;

		String mm = Integer.toString(calendar.get(Calendar.MONTH) + 1);
		if (mm.length() == 1)
			mm = "0" + mm;

		int yy = calendar.get(Calendar.YEAR);
		return mm + "/" + dd + "/" + yy;
	}

	/**
	 * Replace \n with newline, \t with tab in the string. Also unescape '<'.
	 * 
	 * @param str
	 *            string to be processed.
	 * @return string with newline and tab escape sequence replaced.
	 */
	public static String parseString(String str) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '\\') {
				i++;
				if (i < str.length()) {
					if (str.charAt(i) == 't') {
						buf.append("\t");
					} else if (str.charAt(i) == 'n') {
						buf.append("\n");
					} else if (str.charAt(i) == '<') {
						buf.append("<");
					}
				}
			} else {
				buf.append(str.charAt(i));
			}
		}
		return buf.toString();
	}

	public static String escapeApostrophies(String str) {
		if (str == null)
			return null;
		StringBuffer buf = new StringBuffer();
		System.out.println("String str = " + str);
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '\'') {
				buf.append('\'');
				buf.append('\'');
			} else {
				buf.append(str.charAt(i));
			}
		}
		return (buf.toString());
	}

	/**
	 * Replace double single quotes ''with a single one.
	 * 
	 * @param str
	 *            string to be processed.
	 * @return string with a single quote replaced.
	 */
	public static String unescapeApostrophies(String str) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '\'') {
				i++;
				if (i < str.length()) {
					if (str.charAt(i) == '\'') {
						buf.append('\'');
					}
				}
			} else {
				buf.append(str.charAt(i));
			}
		}
		return buf.toString();
	}

	public static String doubleToString(double inValue, int precision) {
		return doubleToString(inValue, precision, false);
	}

	public static String doubleToString(double inValue, int precision,
			boolean useComma) {
		boolean trailingZero;
		double absval = Math.abs(inValue); // getValue positive portion
		if (precision < 0) {
			precision = -precision;
			trailingZero = false;
		} else
			trailingZero = true;
		String signStr = "";
		if (inValue < 0)
			signStr = "-";
		long intDigit = (long) Math.floor(absval); // getValue integer part
		String intDigitStr = String.valueOf(intDigit);

		if (useComma) {
			int intDigitStrLen = intDigitStr.length();
			int digIndex = (intDigitStrLen - 1) % 3;
			digIndex++;
			String intCommaDigitStr = intDigitStr.substring(0, digIndex);
			while (digIndex < intDigitStrLen) {
				intCommaDigitStr += ","
						+ intDigitStr.substring(digIndex, digIndex + 3);
				digIndex += 3;
			}
			intDigitStr = intCommaDigitStr;
		}

		String precDigitStr = "";
		long precDigit = Math.round((absval - intDigit)
				* Math.pow(10.0, precision));
		precDigitStr = String.valueOf(precDigit);

		// pad zeros between decimal and precision digits
		String zeroFilling = "";
		for (int i = 0; i < precision - precDigitStr.length(); i++)
			zeroFilling += "0";
		precDigitStr = zeroFilling + precDigitStr;
		if (!trailingZero) {
			int lastZero;
			for (lastZero = precDigitStr.length() - 1; lastZero >= 0; lastZero--)
				if (precDigitStr.charAt(lastZero) != '0')
					break;
			precDigitStr = precDigitStr.substring(0, lastZero + 1);
		}
		if (precDigitStr.equals(""))
			return signStr + intDigitStr;
		else
			return signStr + intDigitStr + "." + precDigitStr;
	}

	/**
	 * Makes an arry from the parameter enumeration <code>e</code>.
	 * 
	 * @param e
	 *            an enumeration
	 * @return an arry of objects in the underlying enumeration <code>e</code>
	 */
	static public Object[] makeArray(final Enumeration e) {
		ArrayList objs = new ArrayList();
		while (e.hasMoreElements()) {
			objs.add(e.nextElement());
		}
		return objs.toArray();
	}

	/**
	 * Make an Enumeration from the underkying arryay <code>obj</code>
	 * 
	 * @param obj
	 * @return
	 */
	static public Enumeration makeEnumeration(final Object obj) {
		Class type = obj.getClass();
		if (!type.isArray()) {
			throw new IllegalArgumentException(obj.getClass().toString());
		} else {
			return (new Enumeration() {
				int size = Array.getLength(obj);

				int cursor;

				public boolean hasMoreElements() {
					return (cursor < size);
				}

				public Object nextElement() {
					return Array.get(obj, cursor++);
				}
			});
		}
	}

	public static Properties loadConfiguration(String filename)
			throws IOException {
		return loadConfiguration(null, filename);
	}

	public static Properties loadConfiguration(Properties props, String filename)
			throws IOException {
		InputStream is = SorcerUtil.class.getResourceAsStream(filename);
		if (is == null)
			is = (InputStream) (new FileInputStream(new File(filename)));
		if (is != null) {
			props = (props == null) ? new Properties() : props;
			props.load(is);
		} else {
			logger.info("Not able to open stream on properties " + filename);
		}
		return props;
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

	/**
	 * Gets an exception's stack trace as a String
	 * 
	 * @param e
	 *            the exception
	 * @return the stack trace of the exception
	 */
	public static String stackTraceToString(Throwable e) {
		if (e == null) {
			return "";
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(stream);
		e.printStackTrace(writer);
		writer.flush();
		return stream.toString();
	}

	public static String[] stackTraceToArray(Throwable e) {
		String str = stackTraceToString(e);
		return SorcerUtil.tokenize(str, "\t\n\r");
	}

	public static long memoryOccupied() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/**
	 * Returns a CSV string representation of recursive arrays of any component
	 * multitype.
	 * 
	 * @param array
	 *            - an arry of object
	 * @return
	 */
	public static String arrayToCSV(Object array) {
		if (array == null)
			return "";
		else if (!array.getClass().isArray())
			return array.toString();

		int length = Array.getLength(array);
		if (length == 0)
			return "";

		StringBuffer buffer = new StringBuffer("" + Array.get(array, 0));
		int last = length - 1;
		Object obj;
		for (int i = 1; i < length; i++) {
			obj = Array.get(array, i);
			if (obj != null && obj.getClass().isArray())
				buffer.append(arrayToCSV(obj));
			else
				buffer.append(obj);

			if (i != last)
				buffer.append(",");
		}
		return buffer.toString();
	}

	public static Object clone(Object o) throws IOException {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			oos.flush();
			ByteArrayInputStream bis = new ByteArrayInputStream(bos
					.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bis);
			return ois.readObject();
		} catch (ClassNotFoundException cnfe) {
			// impossible
			cnfe.printStackTrace();
			throw new IOException("Could not de-serialize reason:"
					+ cnfe.getMessage());
		}
	}

	/**
	 * Gets an Vector of String in position pos from supplied Vector in which
	 * Strings seperated by sep
	 */
	public static Vector getItems(Vector collection, int position, String sep) {
		Vector items = new Vector();
		Enumeration e = collection.elements();
		while (e.hasMoreElements()) {
			items.addElement(getItem((String) e.nextElement(), position, sep));
		}
		return items;
	}

	public static String getItem(String descriptor, int position, String sep) {
		String[] tokens = tokenize(descriptor, sep);
		return tokens[position];
	}

	public static final String PROTOCOL = "sorcer";

	public static URI getURI(String prvInterface, String prvName,
			Hashtable params) {
		StringBuffer query = new StringBuffer();
		if (query != null) {
			String key;
			for (Enumeration e = params.keys(); e.hasMoreElements();)
				query.append(key = (String) e.nextElement()).append("=")
						.append((String) params.get(key)).append("&");
		}

		if (query.length() > 0)
			query.deleteCharAt(query.length() - 1);

		try {
			return new URI(PROTOCOL, null, "/" + prvInterface + "/" + prvName,
					query.toString(), null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Vector getKeys(Hashtable hash) {
		if (hash == null)
			return null;
		Vector v = new Vector();
		for (Enumeration e = hash.keys(); e.hasMoreElements();)
			v.addElement(e.nextElement());
		return v;
	}
	
	/**
	 * Copy in to out stream. Do not allow other threads to read from the input
	 * or write to the output while copying is taking place.
	 */
	synchronized public static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[256];
		int bytesRead;
		while (true) {
			bytesRead = in.read(buffer);
			if (bytesRead == -1)
				break;
			out.write(buffer, 0, bytesRead);
		}
	}

}
