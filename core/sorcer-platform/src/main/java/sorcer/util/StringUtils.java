/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

public class StringUtils {
	private static Calendar calendar = null;
    final static String CPS = "/";

    public static String[] pathToArray(String arg) {
        StringTokenizer token = new StringTokenizer(arg, CPS);
        String[] array = new String[token.countTokens()];
        int i = 0;
        while (token.hasMoreTokens()) {
            array[i] = token.nextToken();
            i++;
        }
        return (array);
    }

    public static String arrayToString(Object array) {
        return arrayToString(array, 20);
    }

	/**
	 * Returns a string representation of recursive arrays of any component
	 * multitype. in the form [e1,...,ek]
	 */
	public static String arrayToString(Object array, int maxElements) {
		if (array == null)
			return "null";
		else if (!array.getClass().isArray()) {
			return array.toString();
		}
		int length = Array.getLength(array);
		if (length == 0)
			return "[no elements]";
        boolean addTally = false;
        if (length <= maxElements) {
           maxElements = length;
        } else
            addTally = true;

		StringBuilder buffer = new StringBuilder("[");
		int last = maxElements - 1;
		Object obj;
		for (int i = 0; i < maxElements; i++) {
			obj = Array.get(array, i);
			if (obj == null)
				buffer.append("null");
			else if (obj.getClass().isArray())
				buffer.append(arrayToString(obj, maxElements));
			else
				buffer.append(obj);

			if (i == last)
                if (addTally) buffer.append("...{n=").append(length).append("}]");
                else buffer.append("]");
			else
				buffer.append(",");
		}

		return buffer.toString();
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

    public static String toPath(String[] array) {
        if (array.length > 0) {
            StringBuilder sb = new StringBuilder(array[0]);
            for (int i = 1; i < array.length; i++) {
                sb.append(CPS).append(array[i]);
            }
            return sb.toString();
        } else
            return null;
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

    public static String[]tokenizerSplit(String string, String delimiter){
        StringTokenizer token = new StringTokenizer(string, delimiter);
        String[] array = new String[token.countTokens()];
        int i = 0;
        while (token.hasMoreTokens()) {
            array[i] = token.nextToken();
            i++;
        }
        return (array);
    }

    public static String firstToken(String str, String delim) {
		String out = "";

		try {
			CSVStringTokenizer token = new CSVStringTokenizer(str, delim);
			out = token.nextToken();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static String secondToken(String str, String delim) {
		String out = "";

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
		String out = "";

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

	public static void bubbleSort(List coll) {
		int i = coll.size();
		while (--i >= 0) {
			for (int j = 0; j < i; j++) {
				if (((String) coll.get(j)).compareTo((String) coll
						.get(j + 1)) > 0) {
					/* swap objects */
					Object temp = coll.get(j);
					coll.set(j, coll.get(j + 1));
					coll.set(j + 1, temp);
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
		StringBuilder sb = new StringBuilder();
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
		StringBuilder sb = new StringBuilder();
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

		StringBuilder buffer = new StringBuilder("" + Array.get(array, 0));
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

    public static int firstInteger(int defVal, String... strings) {
        for (String string : strings) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                //deliberately ignored, return the first string that parses cleanly
            }
        }
        return defVal;
    }

    public static String tName(String newThreadName) {
        return "[" + Thread.currentThread().getName() + "] " + newThreadName;
    }

    /* Copied from commons-lang3 */

    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * A {@code null} separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by
     * empty strings.</p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param array  the array of values to join together, may be null
     * @param separator  the separator character to use, null treated as ""
     * @return the joined String, {@code null} if null array input
     */
    public static String join(Object[] array, String separator) {
        if (array == null) {
            return null;
        }
        return join(array, separator, 0, array.length);
    }

    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * A {@code null} separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by
     * empty strings.</p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param array  the array of values to join together, may be null
     * @param separator  the separator character to use, null treated as ""
     * @param startIndex the first index to start joining from.  It is
     * an error to pass in an end index past the end of the array
     * @param endIndex the index to stop joining from (exclusive). It is
     * an error to pass in an end index past the end of the array
     * @return the joined String, {@code null} if null array input
     */
    public static String join(Object[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        if (separator == null) {
            separator = "";
        }

        // endIndex - startIndex > 0:   Len = NofStrings *(len(firstString) + len(separator))
        //           (Assuming that all Strings are roughly equally long)
        int noOfItems = endIndex - startIndex;
        if (noOfItems <= 0) {
            return "";
        }

        StringBuilder buf = new StringBuilder(noOfItems * 16);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    /**
     * <p>Joins the elements of the provided {@code Iterator} into
     * a single String containing the provided elements.</p>
     *
     * <p>No delimiter is added before or after the list. Null objects or empty
     * strings within the iteration are represented by empty strings.</p>
     *
     * <p>See the examples here: {@link #join(Object[],char)}. </p>
     *
     * @param iterator  the {@code Iterator} of values to join together, may be null
     * @param separator  the separator character to use
     * @return the joined String, {@code null} if null iterator input
     * @since 2.0
     */
    public static String join(Iterator<?> iterator, char separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return toString(first);
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            buf.append(separator);
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }

        return buf.toString();
    }

    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * Null objects or empty strings within the array are represented by
     * empty strings.</p>
     *
     * <pre>
     * StringUtils.join(null, *)               = null
     * StringUtils.join([], *)                 = ""
     * StringUtils.join([null], *)             = ""
     * StringUtils.join(["a", "b", "c"], ';')  = "a;b;c"
     * StringUtils.join(["a", "b", "c"], null) = "abc"
     * StringUtils.join([null, "", "a"], ';')  = ";;a"
     * </pre>
     *
     * @param array  the array of values to join together, may be null
     * @param separator  the separator character to use
     * @return the joined String, {@code null} if null array input
     * @since 2.0
     */
    public static String join(Object[] array, char separator) {
        if (array == null) {
            return null;
        }

        return join(array, separator, 0, array.length);
    }

    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * Null objects or empty strings within the array are represented by
     * empty strings.</p>
     *
     * <pre>
     * StringUtils.join(null, *)               = null
     * StringUtils.join([], *)                 = ""
     * StringUtils.join([null], *)             = ""
     * StringUtils.join(["a", "b", "c"], ';')  = "a;b;c"
     * StringUtils.join(["a", "b", "c"], null) = "abc"
     * StringUtils.join([null, "", "a"], ';')  = ";;a"
     * </pre>
     *
     * @param array  the array of values to join together, may be null
     * @param separator  the separator character to use
     * @param startIndex the first index to start joining from.  It is
     * an error to pass in an end index past the end of the array
     * @param endIndex the index to stop joining from (exclusive). It is
     * an error to pass in an end index past the end of the array
     * @return the joined String, {@code null} if null array input
     * @since 2.0
     */
    public static String join(Object[] array, char separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        int noOfItems = endIndex - startIndex;
        if (noOfItems <= 0) {
            return "";
        }

        StringBuilder buf = new StringBuilder(noOfItems * 16);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }


    /**
     * <p>Joins the elements of the provided {@code Iterator} into
     * a single String containing the provided elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * A {@code null} separator is the same as an empty String ("").</p>
     *
     * <p>See the examples here: {@link #join(Object[], String)}. </p>
     *
     * @param iterator  the {@code Iterator} of values to join together, may be null
     * @param separator  the separator character to use, null treated as ""
     * @return the joined String, {@code null} if null iterator input
     */
    public static String join(Iterator<?> iterator, String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return toString(first);
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }

    /**
     * <p>Joins the elements of the provided {@code Iterable} into
     * a single String containing the provided elements.</p>
     *
     * <p>No delimiter is added before or after the list. Null objects or empty
     * strings within the iteration are represented by empty strings.</p>
     *
     * <p>See the examples here: {@link #join(Object[],char)}. </p>
     *
     * @param iterable  the {@code Iterable} providing the values to join together, may be null
     * @param separator  the separator character to use
     * @return the joined String, {@code null} if null iterator input
     * @since 2.3
     */
    public static String join(Iterable<?> iterable, char separator) {
        if (iterable == null) {
            return null;
        }
        return join(iterable.iterator(), separator);
    }

    /**
     * <p>Joins the elements of the provided {@code Iterable} into
     * a single String containing the provided elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * A {@code null} separator is the same as an empty String ("").</p>
     *
     * <p>See the examples here: {@link #join(Object[], String)}. </p>
     *
     * @param iterable  the {@code Iterable} providing the values to join together, may be null
     * @param separator  the separator character to use, null treated as ""
     * @return the joined String, {@code null} if null iterator input
     * @since 2.3
     */
    public static String join(Iterable<?> iterable, String separator) {
        if (iterable == null) {
            return null;
        }
        return join(iterable.iterator(), separator);
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
