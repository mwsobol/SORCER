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

package sorcer.core.context.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.service.Evaluation;
import sorcer.service.EvaluationException;
import sorcer.service.modeling.Functionality;
import sorcer.util.GenericUtil;
import sorcer.util.Sorcer;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static sorcer.core.SorcerConstants.DOC_ROOT_DIR;
import static sorcer.core.SorcerConstants.P_DATA_DIR;
/**
 * The class <code>ContextNode</code> is a wrapper for externally persisted
 * data, for example in files or databases. The data is exposed via references
 * called "data items" that can be used as arguments to a setters (setItemValue)
 * and getter (getItemValue) in externally persisted data.
 */
public class ContextNode implements Serializable {
	private static Logger logger = LoggerFactory.getLogger(ContextNode.class);
	private static final long serialVersionUID = 3597662074450280684L;

	private String name;

	private String da = ServiceContext.DA_IN;

	private String mimetype;

	private boolean isTransient = false;

	private Map itemData = new HashMap();

	private String uid;

	private int dataType;

	// data store for this context node
	private Object data;

	// current eval for this context node

	private Object value;
	// variables used to getValue values from the corresponding dependent variables
	protected Map<String, Functionality> variables;

	public ContextNode() {
		// empty context node
	}

	public ContextNode(String name) {
		this.name = name;
	}

	// When the data within a context node is a DB object/or some other object
	public ContextNode(String name, Object data) {
		this(name);
		this.data = data;
	}

	public ContextNode(String name, Object data, String mimetype) {
		this(name, data);
		this.mimetype = mimetype;
	}

	public ContextNode(String name, Object data, String mimetype, boolean isTran) {
		this(name, data, mimetype);
		this.isTransient = isTran;
	}

	// Method to set the datatype of the node's data
	// The multitype should be one of the types in java.sql.Types.
	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public int getDataType() {
		return dataType;
	}

	public String getDataTypeString() {
		return data.getClass().getSimpleName();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public String getDA() {
		return da;
	}

	public void setDA(String directionaAttribute) {
		da = directionaAttribute;
	}

	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	public boolean isTransient() {
		return this.isTransient;
	}

	public Object getData() throws MalformedURLException {
		if ((data instanceof URL) || (data instanceof String)) {
			if (isURL())
				return new URL(data.toString());
		}
		return data;
	}

	public boolean isIn() {
		return da.equals(ServiceContext.DA_IN);
	}

	public boolean isOut() {
		return da.equals(ServiceContext.DA_OUT);
	}

	public boolean isInout() {
		return da.equals(ServiceContext.DA_INOUT);
	}

	public InputStream openStream() throws IOException, ContextNodeException {
		URL myURL = getURL();
		URLConnection myConnect = myURL.openConnection();
		myConnect.setDoInput(true);
		myConnect.setDoOutput(true);
		myConnect.setUseCaches(false);
		return myConnect.getInputStream();
	}

	public void download(File outFile) throws IOException, ContextNodeException {
		// open streams
		InputStream inStream = openStream();
		FileOutputStream outStream = new FileOutputStream(outFile);

		// read
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = inStream.read(buffer)) != -1) { // EOF
			outStream.write(buffer, 0, bytesRead);
		}

		// close streams
		outStream.close();
		inStream.close();
	}

	public void download(File outFile, URL url) throws IOException, ContextNodeException {
		this.download(outFile);
		data = url;
	}

	public String toString() {
		// return key + " (" + data + ")";
		return toFullString();
	}

	public String toFullString() {
		StringBuilder result = new StringBuilder();
		if (!isEmpty())
			result.append(name).append(" (").append(data).append(", ").append(
					"IO Type = ").append(da).append(", Transient = ").append(
					isTransient() ? "True" : "False").append(")");
		else
			result.append(name).append(ServiceContext.EMPTY_LEAF);

		return result.toString();
	}

	public String toInfoString() {
		StringBuffer result = new StringBuffer();

		if (!isEmpty())
			result.append("Tag : ").append(name).append("\n").append(
					"Value : ").append(data).append("\n")
					.append("Transient : ").append(
							isTransient() ? "True" : "False").append("\n")
					.append("IO Type : ").append(da);
		else
			result.append("Tag : ").append(name).append(
					ServiceContext.EMPTY_LEAF);
		return result.toString();
	}

	public String getLabel() {
		// Used For all Views.
		if (!isEmpty())
			return name;
		else
			return name + ServiceContext.EMPTY_LEAF;
	}

	public String toStringDeep(String tab) {
		return (tab + toString());
	}

	public String toStringDeep(String indent, String tab) {
		return (indent + toString());
	}

	public boolean isCRL() {
		if (data instanceof String) {
			if ((data.toString()).startsWith("ctxt://"))
				return true;
		}
		return false;
	}

	public boolean isURL() {
		if (isHttp())
			return true;
		if (isFile())
			return true;
		if (isFTP())
			return true;
		return false;
	}

	public URL getURL() throws ContextNodeException, MalformedURLException {

		if (!isURL())
			throw new ContextNodeException(
					"ContextNode does not contain a URL string or URL object.");
		String urlValue = data.toString();
		// return new URL((String)this.eval);
		return new URL(urlValue);
	}

	public String getHttpUrl() throws ContextNodeException,
			MalformedURLException {
		if (!isURL())
			throw new ContextNodeException(
					"ContextNode does not contain a URL string or URL object.");
		String urlValue = data.toString();
		if (urlValue.startsWith("/") || urlValue.charAt(1) == ':') {
			String dir = ""+Sorcer.getDataDir();
			int index = urlValue.indexOf(dir);
			if (index >= 0) {
				urlValue = urlValue.substring(index + dir.length());
				urlValue = Sorcer.getDataServerUrl() + '/' + urlValue;
			}
		}
		return urlValue;
	}

	public boolean isHttp() {
		if (data==null) return false;
		String urlValue = data.toString();
		if (urlValue.startsWith("http://"))
			return true;
		return false;
	}

	public boolean isFile() {
		if (data==null) return false;
		String urlValue = data.toString();
		if (urlValue.startsWith("file://"))
			return true;
		return false;
	}

	public boolean isFTP() {
		if (data==null) return false;
		String urlValue = data.toString();
		if (urlValue.startsWith("ftp://"))
			return true;
		return false;
	}

	public void addItem(String itemName, List itemDataVect) {
		itemData.put(itemName, itemDataVect);
	}

	public void addVar(String itemName, Functionality var)
			throws ContextNodeException {

		if (hasItem(itemName) == false) {
			throw new ContextNodeException("Cannot add Variable; itemName = \""
					+ itemName + "\" is not defined in " + "this ContextNode.");
		}

		if (variables.containsKey(var.getName()) == true) {
			throw new ContextNodeException(
					"Cannot add Variable; Variable already " + "added.");

		}

		variables.put(itemName, var);

	}

	public void removeVar(String itemName) {
		variables.remove(itemName);
	}

	private boolean hasVars() {
		if (variables.size() > 0)
			return true;
		return false;
	}

	public void removeItem(String itemName) {
		itemData.remove(itemName);
		removeVar(itemName);
	}

	public Map getItemData() {
		return itemData;
	}

	public Vector getItemDataVect(String itemName) {
		return (Vector) (itemData.get(itemName));
	}

	public void setItemValue(String itemName, Object obj) throws IOException,
			ContextNodeException {

		Vector itemDataVect = (Vector) itemData.get(itemName);

		if (itemDataVect.elementAt(0).equals("File")) {
			int line = ((Integer) itemDataVect.elementAt(2)).intValue();
			int field = ((Integer) itemDataVect.elementAt(3)).intValue();
			String delimiter = (String) itemDataVect.elementAt(4);
			this.setFileItemValue(obj, line, field, delimiter);

			int ctr = 0;
			Object obj2 = this.getItemValue(itemName);
			logger.info("setItemValue:\n"
					+ "Checking to see if the set() took "
					+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
					+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
					+ obj2 + "\"");

			while (ctr < 240 && !(((obj.toString())).equals(obj2.toString()))) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				ctr++;
				obj2 = this.getItemValue(itemName);

				logger.info("setItemValue:\n"
						+ "Checking to see if the set() took "
						+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
						+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
						+ obj2 + "\"");

				System.out.print(".");
			}
			System.out.println();

			if (ctr == 120)
				throw new ContextNodeException("\nContextNode.setItemValue(): "
						+ "There is a problem " + "setting eval.");

			logger.info("Network latency = " + (ctr * 0.5) + " seconds.\n");
		}
		if (itemDataVect.elementAt(0).equals("Keyword Filter")) {

			String keyword = (String) itemDataVect.elementAt(2);
			int field = ((Integer) itemDataVect.elementAt(3)).intValue();
			String delimiter = (String) itemDataVect.elementAt(4);

			this.setFileItemValue(obj, keyword, field, delimiter);

			int ctr = 0;
			Object obj2 = this.getItemValue(itemName);
			logger.info("setItemValue:\n"
					+ "Checking to see if the set() took "
					+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
					+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
					+ obj2 + "\"");

			while (ctr < 240 && !(((obj.toString())).equals(obj2.toString()))) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				ctr++;
				obj2 = this.getItemValue(itemName);

				logger.info("setItemValue:\n"
						+ "Checking to see if the set() took "
						+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
						+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
						+ obj2 + "\"");

				System.out.print(".");
			}
			System.out.println();

			if (ctr == 120)
				throw new ContextNodeException("\nContextNode.setItemValue(): "
						+ "There is a problem " + "setting eval.");

			logger.info("Network latency = " + (ctr * 0.5) + " seconds.\n");
		}
		if (itemDataVect.elementAt(0).equals("Keyword Filter2")) {

			String keyword = (String) itemDataVect.elementAt(2);
			int field = ((Integer) itemDataVect.elementAt(3)).intValue();
			String delimiter = (String) itemDataVect.elementAt(4);
			int subField = ((Integer) itemDataVect.elementAt(5)).intValue();
			String subDelimiter = (String) itemDataVect.elementAt(6);

			this.setFileItemValue(obj, keyword, field, delimiter, subField,
					subDelimiter);
			int ctr = 0;
			Object obj2 = this.getItemValue(itemName);
			logger.info("setItemValue:\n"
					+ "Checking to see if the set() took "
					+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
					+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
					+ obj2 + "\"");

			while (ctr < 240 && !(((obj.toString())).equals(obj2.toString()))) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				ctr++;
				obj2 = this.getItemValue(itemName);

				logger.info("setItemValue:\n"
						+ "Checking to see if the set() took "
						+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
						+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
						+ obj2 + "\"");

				System.out.print(".");
			}
			System.out.println();

			if (ctr == 120)
				throw new ContextNodeException("\nContextNode.setItemValue(): "
						+ "There is a problem " + "setting eval.");

			logger.info("Network latency = " + (ctr * 0.5) + " seconds.\n");
		}
		if (itemDataVect.elementAt(0).equals("Keyword Filter3")) {

			String keyword = (String) itemDataVect.elementAt(2);
			this.setFileItemValue(obj, keyword);

		}
		if (itemDataVect.elementAt(0).equals("Keyword Filter4")) {

			String keyword = (String) itemDataVect.elementAt(2);
			int lineAfter = (Integer) itemDataVect.elementAt(3);
			int field = (Integer) itemDataVect.elementAt(4);
			String delimiter = (String) itemDataVect.elementAt(5);

			this.setFileItemValue(obj, keyword, lineAfter, field, delimiter);

			int ctr = 0;
			Object obj2 = this.getItemValue(itemName);
			logger.info("setItemValue:\n"
					+ "Checking to see if the set() took "
					+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
					+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
					+ obj2 + "\"");

			while (ctr < 240 && !(((obj.toString())).equals(obj2.toString()))) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				ctr++;
				obj2 = this.getItemValue(itemName);

				logger.info("setItemValue:\n"
						+ "Checking to see if the set() took "
						+ "hold:\n\titemName=\"" + itemName + "\"\n\t"
						+ "desired eval=\"" + obj + "\"\n\t" + "read eval=\""
						+ obj2 + "\"");

				System.out.print(".");
			}
			System.out.println();

			if (ctr == 120)
				throw new ContextNodeException("\nContextNode.setItemValue(): "
						+ "There is a problem " + "setting eval.");

			logger.info("Network latency = " + (ctr * 0.5) + " seconds.\n");
		}

		if (itemDataVect.elementAt(0).equals("Method")) {
			// compute out the signature of the method
			// assuming the following structure "methodName(Type1 arg1, Type2
			// arg2, ...)"

			String setMethodSignature = (String) itemDataVect.elementAt(2);
			StringTokenizer MethodSignature = new StringTokenizer(
					setMethodSignature, "(");
			String setMethodName = (MethodSignature.nextToken()).trim();
			String setArguments = (MethodSignature.nextToken()).trim();
			// if method has any arguments

			if (!setArguments.equals(")")) {
				// strip off the closing paran

				StringTokenizer setArgs = new StringTokenizer(setArguments, ")");

				String args = (setArgs.nextToken()).trim();

				// create a tokenizer for each typei,argi pair

				StringTokenizer argPair = new StringTokenizer(args, ",");
				int nArgs = argPair.countTokens();
				Class[] argParamTypes = new Class[nArgs];
				Object[] argObjects = new Object[nArgs];
				int tCount = 0;

				while (argPair.hasMoreTokens()) {

					StringTokenizer pair = new StringTokenizer((argPair
							.nextToken()).trim(), " ");
					// compute each pair into multitype eval and instantiate the
					// object
					String type = (pair.nextToken()).trim();
					String argV = (pair.nextToken()).trim();
					// System.out.println("\n**************** Arg multitype, Arg
					// Value "+multitype+" , "+argV);
					// create an instance of the class for this argument
					try {
						Class argVcl = Class.forName(type);
						// getValue the right constrcutor assuming there is a
						// constructor with a single string
						Class[] paramTypes = { argV.getClass() };
						Object[] objA = { obj };
						if (argV.equals("eval")) {
							argObjects[tCount] = obj;
							// check to see if the class of obj matches class of
							// arg list
							// This is due to the fact that the eval may have
							// been obtained from a file as a String as
							// is the case for parametric analysis
							// System.out.println("\n**************** objclass
							// "+obj.getClass());
							// System.out.println("\n**************** argVcl "+
							// argVcl.getClass());
							// System.out.println("\n**************** multitype class
							// "+ Class.forName(multitype));
							if (obj.getClass() == argVcl.getClass()
									|| obj.getClass() == Class.forName(type)) {
								// System.out.println("objclass eq argVclass
								// "+obj.getClass()+","+ argV.getClass());
								argParamTypes[tCount] = obj.getClass();
								argObjects[tCount] = obj;
							} else {
								// try to construct an object of the appropriate
								// fiType based on the argV class fiType
								// System.out.println("\n**************** multitype
								// is "+multitype);
								// System.out.println("\nClass******** is "+
								// Class.forName(multitype));
								// convert the obj to a string assuming that the
								// constructor of the object takes one
								objA[0] = objA[0].toString();
								argParamTypes[tCount] = Class.forName(type);
								argObjects[tCount] = GenericUtil.getInstance(
										type, objA);
							}
							// System.out.println("**************** Arg object
							// "+obj);
							// System.out.println("**************** Arg class
							// "+obj.getClass());
						} else {
							Object[] argVals = { argV };
							// Constructor clCons =
							// cl.getConstructor(paramTypes);
							// Object argObj = clCons.newInstance(argVals);
							Object argObj = GenericUtil.getInstance(type,
									argVals);
							argObjects[tCount] = argObj;
							argParamTypes[tCount] = argObj.getClass();
							// System.out.println("**************** Arg object
							// "+argObj);
							// System.out.println("**************** Arg class
							// "+argObj.getClass());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					tCount = tCount + 1;
				}

				// setClassItemValue(obj,setMethodName);
				setClassItemValue(setMethodName, argParamTypes, argObjects);
			} else {
				Class[] argParamTypes = new Class[1];
				Object[] argObjects = new Object[1];
				argParamTypes[0] = null;
				argObjects[0] = null;
				setClassItemValue(setMethodName, argParamTypes, argObjects);
			}
		}
	}

	private void setFileItemValue(Object obj, String keyword, int field,
			String delimiter) throws IOException, ContextNodeException {
		setFileItemValue(obj, keyword, 0, field, delimiter);

	}

	private void setFileItemValue(Object obj, String keyword)
			throws IOException, ContextNodeException {
		// TODO Auto-generated method stub
		Vector fC = getContentsViaInputStream();
		for (int i = 0; i < fC.size(); i++) {
			fC.set(i, ((String) (fC.get(i))).replace(keyword, obj.toString()));
		}
		setFileContents(fC);
	}

	public Object getItemValue(String itemName) throws ContextNodeException,
			IOException {
		Vector itemDataVect = (Vector) this.itemData.get(itemName);
		if (itemDataVect == null)
			throw new ContextNodeException(
					"Cannot getItemValue() in ContextNode \"" + toString()
							+ "\" for data impl \"" + itemName
							+ "\"; no such data impl.");
		Object obj = null;
		if (itemDataVect.elementAt(0).equals("File")) {
			int line = (Integer) itemDataVect.elementAt(2);
			int field = (Integer) itemDataVect.elementAt(3);
			String delimiter = (String) itemDataVect.elementAt(4);
			obj = this.getFileItemValue(line, field, delimiter);
			// convert to proper multitype
			if (((String) itemDataVect.elementAt(1)).equals("Double"))
				obj = new Double((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("Integer"))
				obj = new Integer((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("String"))
				obj = obj.toString();
		} else if (itemDataVect.elementAt(0).equals("File2")) {
			int line = (Integer) itemDataVect.elementAt(2);
			int field = (Integer) itemDataVect.elementAt(3);
			String delimiter = (String) itemDataVect.elementAt(4);
			int field2 = (Integer) itemDataVect.elementAt(5);
			String delimiter2 = (String) itemDataVect.elementAt(6);

			// System.out.println("Line: "+line);
			// System.out.println("Field: "+field);
			// System.out.println("Delimiter: "+delimiter);

			obj = this.getFileItemValue(line, field, delimiter, field2,
					delimiter2);
			// convert to proper multitype
			if (((String) itemDataVect.elementAt(1)).equals("Double"))
				obj = new Double((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("Integer"))
				obj = new Integer((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("String"))
				obj = obj.toString();
		} else if (itemDataVect.elementAt(0).equals("Keyword Filter")) {
			String keyword = (String) itemDataVect.elementAt(2);
			int field = (Integer) itemDataVect.elementAt(3);
			String delimiter = (String) itemDataVect.elementAt(4);

			// System.out.println("Line: "+line);
			// System.out.println("Field: "+field);
			// System.out.println("Delimiter: "+delimiter);

			obj = this.getFileItemValue(keyword, field, delimiter);
			// convert to proper multitype
			if (((String) itemDataVect.elementAt(1)).equals("Double"))
				obj = new Double((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("Integer"))
				obj = new Integer((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("String"))
				obj = obj.toString();
		} else if (itemDataVect.elementAt(0).equals("Keyword Filter2")) {
			String keyword = (String) itemDataVect.elementAt(2);
			int field = ((Integer) itemDataVect.elementAt(3)).intValue();
			String delimiter = (String) itemDataVect.elementAt(4);
			int subField = ((Integer) itemDataVect.elementAt(5)).intValue();
			String subDelimiter = (String) itemDataVect.elementAt(6);

			// System.out.println("Line: "+line);
			// System.out.println("Field: "+field);
			// System.out.println("Delimiter: "+delimiter);

			obj = this.getFileItemValue(keyword, field, delimiter, subField,
					subDelimiter);
			// convert to proper multitype
			if (((String) itemDataVect.elementAt(1)).equals("Double"))
				obj = new Double((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("Integer"))
				obj = new Integer((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("String"))
				obj = obj.toString();
		} else if (itemDataVect.elementAt(0).equals("Keyword Filter3")) {
			String keyword = (String) itemDataVect.elementAt(2);
			// this filter does not have the ability to getValue once the string is
			// replaced
			obj = this.getFileItemValue(keyword);
		} else if (itemDataVect.elementAt(0).equals("Keyword Filter4")) {
			String keyword = (String) itemDataVect.elementAt(2);
			int lineAfter = ((Integer) itemDataVect.elementAt(3)).intValue();
			int field = ((Integer) itemDataVect.elementAt(4)).intValue();
			String delimiter = (String) itemDataVect.elementAt(5);

			// System.out.println("Line: "+line);
			// System.out.println("Field: "+field);
			// System.out.println("Delimiter: "+delimiter);

			obj = this.getFileItemValue(keyword, lineAfter, field, delimiter);
			// convert to proper multitype
			if (((String) itemDataVect.elementAt(1)).equals("Double"))
				obj = new Double((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("Integer"))
				obj = new Integer((String) obj);
			if (((String) itemDataVect.elementAt(1)).equals("String"))
				obj = obj.toString();
		} else if (itemDataVect.elementAt(0).equals("Method")) {
			// compute out the signature of the method
			// assuming the following structure "methodName(Type1 arg1, Type2
			// arg2, ...)"
			// NOTE: The Typei have to be java objects and fully qualified

			String getMethodSignature = (String) itemDataVect.elementAt(3);
			// System.out.println("Method Signature "+getMethodSignature);
			StringTokenizer MethodSignature = new StringTokenizer(
					getMethodSignature, "(");
			String getMethodName = (MethodSignature.nextToken()).trim();
			// System.out.println("getMethodName "+getMethodName);

			String getArguments = (MethodSignature.nextToken()).trim();
			// System.out.println("getArguments "+getArguments);

			// if methods has any arguments

			if (!getArguments.equals(")")) {
				// System.out.println("method has arguments");

				// strip off the closing paran

				StringTokenizer getArgs = new StringTokenizer(getArguments, ")");
				String args = (getArgs.nextToken()).trim();

				// create a tokenizer for each typei,argi pair

				StringTokenizer argPair = new StringTokenizer(args, ",");

				int nArgs = argPair.countTokens();
				Class[] argParamTypes = new Class[nArgs];
				Object[] argObjects = new Object[nArgs];
				int tCount = 0;
				while (argPair.hasMoreTokens()) {

					StringTokenizer pair = new StringTokenizer((argPair
							.nextToken()).trim(), " ");

					// compute each pair into multitype eval and instantiate the
					// object
					String type = (pair.nextToken()).trim();
					String argV = (pair.nextToken()).trim();
					// create an instance of the class for this argument
					try {
						// Class cl = Class.forName(multitype);
						// getValue the right constrcutor assuming there is a
						// constructor with a single string
						// Class[] paramTypes = {argV.getClass()};
						Object[] argVals = { argV };
						// Constructor clCons = cl.getConstructor(paramTypes);
						// Object argObj = clCons.newInstance(argVals);
						Object argObj = GenericUtil.getInstance(type, argVals);
						argObjects[tCount] = argObj;
						argParamTypes[tCount] = argObj.getClass();
						// System.out.println("**************** Arg object
						// "+argObj);
						// System.out.println("**************** Arg class
						// "+argObj.getClass());

					} catch (Exception e) {
						e.printStackTrace();
					}
					tCount = tCount + 1;
				}

				obj = getClassItemValue(getMethodName, argParamTypes,
						argObjects);
			} else {
				// System.out.println("**************** calling
				// getClassItemValue Method is "+getMethodName);
				Class[] argParamTypes = new Class[1];
				Object[] argObjects = new Object[1];
				argParamTypes[0] = null;
				argObjects[0] = null;
				obj = getClassItemValue(getMethodName, argParamTypes,
						argObjects);
			}
		} else if (itemDataVect.elementAt(0).equals("JEP")) {
			try {
				//obj = ((ExpressionEvaluator) data).execute(itemName);
				obj = ((Evaluation) data).evaluate();
			} catch (EvaluationException e) {
				e.printStackTrace();
			}
		} else {
			obj = null;
		}
		return obj;
	}

	private Object getFileItemValue(String keyword, int lineAfter, int field,
			String delimiter) throws IOException, ContextNodeException {
		Vector fC = this.getContentsViaInputStream();

		int keyLine = -1;

		for (int l = 0; l < fC.size(); l++) {
			if (((String) fC.get(l)).trim().startsWith(keyword))
				keyLine = l + 1;
		}

		if (keyLine == -1) {
			System.out.println("Error: No keyword match found ......");
			throw new ContextNodeException(
					"The data in ContextNode  File does not contain keyword = "
							+ keyword);
		}
		// record with eval is lineAfter keyword line
		String lineX = (String) fC.elementAt(keyLine - 1 + lineAfter);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);

		for (int i = 0; i < field - 1; i++) {
			// System.out.println("i = "+i);
			lineXT.nextToken();
			// System.out.println("Current Token is "+lineXT.nextToken());
		}
		return lineXT.nextToken();
	}

	private Object getFileItemValue(String keyword) {
		// TODO Auto-generated method stub
		// For this multitype of filter which replaces a string with a eval the
		// GetValue() method
		// has no meaning, there is no way to find the original location in the
		// file.
		return keyword;
	}

	public Set getItemNames() {
		return itemData.keySet();
	}

	public boolean hasItem(String itemName) {
		return itemData.containsKey(itemName);
	}

	private Object getFileItemValue(int line, int field, String delimiter)
			throws IOException, ContextNodeException {

		Vector fC = getContentsViaInputStream();
		String lineX = (String) fC.elementAt(line - 1);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);

		// System.out.println("Line = \"" + lineX + "\"");
		// System.out.println(lineXT.toString());

		for (int i = 0; i < field - 1; i++) {
			lineXT.nextToken();
		}
		return lineXT.nextToken();
	}

	private Object getFileItemValue(String key, int field, String delimiter)
			throws IOException, ContextNodeException {
		return getFileItemValue(key, 0, field, delimiter);
		// Vector fC = this.getContentsViaInputStream();
		//
		// int keyLine = -1;
		//
		// for (int l = 0; l < fC.size(); l++) {
		// if (((String) fC.getValue(l)).trim().startsWith(key))
		// keyLine = l + 1;
		// }
		//
		// if (keyLine == -1)
		// System.out.println("Error: No keyword match found ......");
		//
		// String lineX = (String) fC.elementAt(keyLine - 1);
		// StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);
		//
		// for (int i = 0; i < field - 1; i++) {
		// // System.out.println("i = "+i);
		// lineXT.nextToken();
		// // System.out.println("Current Token is "+lineXT.nextToken());
		// }
		// return lineXT.nextToken();
	}

	private Object getFileItemValue(String key, int field, String delimiter,
			int subField, String subDelimiter)

	throws IOException, ContextNodeException {

		int keyLine = -1;

		Vector fC = this.getContentsViaInputStream();

		for (int l = 0; l < fC.size(); l++) {
			if (((String) fC.get(l)).trim().startsWith(key))
				keyLine = l + 1;
		}

		if (keyLine == -1)
			System.out.println("Error: No keyword match found ......");

		String lineX = (String) fC.elementAt(keyLine - 1);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);

		for (int i = 0; i < field - 1; i++) {
			// System.out.println("i = "+i);
			lineXT.nextToken();
			// System.out.println("Current Token is "+lineXT.nextToken());
		}

		StringTokenizer fieldXT = new StringTokenizer(lineXT.nextToken(),
				subDelimiter);
		for (int i = 0; i < subField - 1; i++) {
			fieldXT.nextToken();
		}

		return fieldXT.nextToken();
	}

	private Object getFileItemValue(int line, int field, String delimiter,
			int field2, String delimiter2) throws IOException,
			ContextNodeException {

		Vector fC = this.getContentsViaInputStream();
		String lineX = (String) fC.elementAt(line - 1);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);

		for (int i = 0; i < field - 1; i++) {
			// System.out.println("i = "+i);
			lineXT.nextToken();
			// System.out.println("Current Token is "+lineXT.nextToken());
		}
		StringTokenizer fieldXT = new StringTokenizer(lineXT.nextToken(),
				delimiter2);
		for (int i = 0; i < field2 - 1; i++) {
			fieldXT.nextToken();
		}
		return fieldXT.nextToken();
	}

	private void setClassItemValue(String setMethodName, Class[] paramTypes,
			Object[] params) throws ContextNodeException {
		Class cl = data.getClass();
		Method setMethod = null;
		try {
			if (paramTypes[0] == null) {
				// System.out.println("Method Tag is"+setMethodName);
				setMethod = cl.getMethod(setMethodName, (Class[]) null);
			} else {
				// System.out.println("Method Tag is"+setMethodName);
				// System.out.println("cls0 = "+paramTypes[0]);
				// System.out.println("cls1 = "+paramTypes[1]);
				setMethod = cl.getMethod(setMethodName, paramTypes);
			}
			if (params[0] == null) {
				setMethod.invoke(data, (Object[]) null);
			} else {
				setMethod.invoke(data, params);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return;
	}

	private Object getClassItemValue(String getMethodName,
			Class[] argParamTypes, Object[] argObjects)
			throws ContextNodeException {
		// for (int i = 0; i<argParamTypes.length;
		// i++)System.out.println("paramtype,obj"+argParamTypes[i]+" ,
		// "+argObjects[i]);
		Object value = null;
		// System.out.println("datavalue is"+data);
		Class cl = data.getClass();
		Method getMethod = null;
		// System.out.println("Class is"+cl.getName());
		try {
			if (argParamTypes[0] == null) {
				getMethod = cl.getMethod(getMethodName, (Class[]) null);
			} else {
				getMethod = cl.getMethod(getMethodName, argParamTypes);
			}
			// System.out.println("method is "+getMethod);
			if (argObjects[0] == null) {
				value = getMethod.invoke(data, (Object[]) null);
			} else {
				value = getMethod.invoke(data, argObjects);
			}
			// System.out.println("eval is "+eval);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	private void setFileItemValue(Object value, int line, int field,
			String delimiter) throws IOException, ContextNodeException {

		Vector fC = getContentsViaInputStream();
		String lineX = (String) fC.elementAt(line - 1);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);
		String newLine = new String("");
		int n = lineXT.countTokens();
		for (int i = 0; i < n; i++) {
			if (i == field - 1) {
				newLine = newLine + value.toString();
				lineXT.nextToken();
			} else {
				newLine = newLine + lineXT.nextToken();
			}
			if (lineXT.hasMoreTokens()) {
				newLine = newLine + delimiter;
			}
		}

		fC.setElementAt(newLine, line - 1);

		setFileContents(fC);
	}

	private void setFileItemValue(Object value, String key, int lineAfter,
			int field, String delimiter) throws IOException,
			ContextNodeException {

		int keyLine = -1;

		Vector fC = getContentsViaInputStream();

		for (int l = 0; l < fC.size(); l++) {
			if (((String) fC.get(l)).trim().startsWith(key))
				keyLine = l + 1;
		}

		if (keyLine == -1)
			System.out.println("Error: No keyword match found ......");

		String lineX = (String) fC.elementAt(keyLine - 1 + lineAfter);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);

		String newLine = new String("");
		// for (int i=0; i<fC.size(); i++) {
		// System.out.println(fC.elementAt(i).toString());
		// }
		int n = lineXT.countTokens();

		for (int i = 0; i < n; i++) {
			if (i == field - 1) {
				newLine = newLine + value.toString();
				String nToken = lineXT.nextToken();
			} else {
				newLine = newLine + lineXT.nextToken();
			}

			if (lineXT.hasMoreTokens()) {
				newLine = newLine + delimiter;
			}
		}
		fC.setElementAt(newLine, keyLine - 1 + lineAfter);

		this.setFileContents(fC);
		// setContentsViaOutputStream(fC);

		// for (int i=0; i<fC.size(); i++) {
		// System.out.println(fC.elementAt(i).toString());
		// }
	}

	private void setFileItemValue(Object value, String key, int field,
			String delimiter, int subField, String subDelimiter)
			throws IOException, ContextNodeException {

		int keyLine = -1;

		Vector fC = getContentsViaInputStream();

		for (int l = 0; l < fC.size(); l++) {
			if (((String) fC.get(l)).trim().startsWith(key))
				keyLine = l + 1;
		}

		if (keyLine == -1)
			System.out.println("Error: No keyword match found ......");

		String lineX = (String) fC.elementAt(keyLine - 1);
		StringTokenizer lineXT = new StringTokenizer(lineX, delimiter);

		String newLine = new String("");
		// for (int i=0; i<fC.size(); i++) {
		// System.out.println(fC.elementAt(i).toString());
		// }
		int n = lineXT.countTokens();

		for (int i = 0; i < n; i++) {
			if (i == field - 1) {
				String nToken = lineXT.nextToken();

				StringTokenizer fieldXT = new StringTokenizer(nToken,
						subDelimiter);

				int nField = fieldXT.countTokens();

				for (int j = 0; j < nField; j++) {
					if (j == subField - 1) {
						newLine = newLine + value.toString() + subDelimiter;
						fieldXT.nextToken();
					} else {
						newLine = newLine + fieldXT.nextToken() + subDelimiter;
					}
					// added subDelimiter above
					// if (fieldXT.hasMoreTokens()) {
					// newLine = newLine + subDelimiter;
					// }
				}
			} else {
				newLine = newLine + lineXT.nextToken();
			}

			if (lineXT.hasMoreTokens()) {
				newLine = newLine + delimiter;
			}
		}

		fC.setElementAt(newLine, keyLine - 1);

		this.setFileContents(fC);
		// setContentsViaOutputStream(fC);

		// for (int i=0; i<fC.size(); i++) {
		// System.out.println(fC.elementAt(i).toString());
		// }
	}

	public Vector getContentsViaInputStream() throws IOException,
			ContextNodeException {
		Vector fileContents = new Vector();
		InputStream is = this.openStream();
		InputStreamReader iSR = new InputStreamReader(is);
		BufferedReader bR = new BufferedReader((Reader) iSR);
		boolean eof = false;

		while (!eof) {
			String line = bR.readLine();
			if (line == null) {
				eof = true;
			} else {
				fileContents.addElement(line);
			}
		}
		is.close();
		return fileContents;
	}

	private boolean dataIsURL() throws MalformedURLException {
		if ((data instanceof String) && (data.toString()).startsWith("http://")) {
			String urlValue = data.toString();
			URL url = new URL(urlValue);
		}
		Boolean myBoo = new Boolean(data instanceof URL);
		Boolean myBoo2;
		if (myBoo.booleanValue()) {
			myBoo2 = new Boolean("false");
		} else {
			myBoo2 = new Boolean("true");
		}
		if (myBoo2.booleanValue()) {
			throw new MalformedURLException("Not valid URL in data node "
					+ name);
		}
		boolean stuff = (data instanceof URL);
		return stuff;
	}

	public boolean isDataSettable() throws ContextNodeException,
			MalformedURLException {
		if ((isFile()) || (isURL())) {
			return isFileWritable();
		}
		return true;
	}

	private boolean isFileWritable() throws ContextNodeException,
			MalformedURLException {
		if ((!(isFile())) && (!(isURL())))
			throw new ContextNodeException(
					"The data in ContextNode is not a File or "
							+ "URL; the method isFileWritable() "
							+ "does not apply.");
		return getFile().canWrite();
	}

	private void setFileContents(Vector fC) throws ContextNodeException,
			MalformedURLException, IOException {

		writeVectorToFile(getFile(), fC);
	}

	private void writeVectorToFile(File dataFile, Vector fC) throws IOException {
		FileWriter out = new FileWriter(dataFile);
		BufferedWriter bwOut = new BufferedWriter(out);

		for (int i = 0; i < fC.size(); i++) {
			String pclLine = (String) fC.elementAt(i);
			bwOut.write(pclLine, 0, pclLine.length());
			bwOut.newLine();
		}

		bwOut.flush();
		out.flush();

		bwOut.close();
		out.close();
	}

	public File getFile() throws MalformedURLException, ContextNodeException {
		File dataFile = null;

		if (isFile()) {
			String urlValue = data.toString();
			URL myURL = new URL(urlValue);
			dataFile = getFileWithPathAdjustment(myURL);
		} else if (isURL()) {
			if (this.data instanceof URL) {
				dataFile = getFileWithPathAdjustment(((URL) data));
			} else {
				String urlValue = data.toString();
				URL myURL = new URL(urlValue);
				dataFile = getFileWithPathAdjustment(myURL);
			}
		}

		StringBuffer sb;
		String msg = "(Ensure node has proper read/write permissions and that path is correct...\nURL must point into "
				+ Sorcer.getProperty(DOC_ROOT_DIR)
				+ "): \nFile: "
				+ dataFile.getAbsolutePath();

		if (!dataFile.canWrite()) {
			sb = new StringBuffer("Cannot write ContextNode ");
			sb.append(msg);
			throw new ContextNodeException(sb.toString());
		}
		if (!dataFile.canRead()) {
			sb = new StringBuffer("Cannot read ContextNode ");
			sb.append(msg);
			throw new ContextNodeException(sb.toString());
		}
		return dataFile;
	}

	private File getFileWithPathAdjustment(URL myURL) {

		String dataRootDir, dataDir, filePath;
		dataRootDir = Sorcer.getProperty(DOC_ROOT_DIR);
		dataDir = Sorcer.getProperty(P_DATA_DIR);
		filePath = myURL.getPath();
		char sep = filePath.charAt(0);
		if (sep != File.separatorChar)
			filePath.replace(sep, File.separatorChar);

		filePath = dataRootDir + filePath;
		return new File(filePath);
	}

	public String getContent() {
		StringBuffer sb = new StringBuffer();
		sb.append("Tag: " + name).append("\n").append("Data: " + data).append(
				"\n").append("I/O Type: " + da).append("\n").append(
				"isTran: " + isTransient).append("\n");
		return sb.toString();
	}

	public String getID() {
		return uid;
	}

	public void copy(ContextNode dN) throws MalformedURLException {
		this.name = dN.getName();
		this.data = dN.getData();
		this.da = dN.getDA();
		this.isTransient = dN.isTransient();
		this.itemData = dN.getItemData();
		this.uid = dN.getID();
	}

	public void isTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	public boolean isEmpty() {
		if (data instanceof String) {
			String strValue = data.toString();
			if (strValue.equals(ServiceContext.EMPTY_LEAF))
				return true;
		}
		return false;
	}

	public boolean isWritableContextNode() throws EvaluationException {
		try {
			return isDataSettable();
		} catch (ContextNodeException e) {
			throw new EvaluationException(
					"Exception checking ContextNode file "
							+ "writing ability; ContextNode: \"" + this + "\".",
					e);
		} catch (MalformedURLException mfE) {
			throw new EvaluationException(
					"Exception checking ContextNode file "
							+ "writing ability (Malformed URL)"
							+ "; ContextNode: \"" + this + "\".", mfE);
		}

	}

	public void setData(Object data) {
		this.data = data;
	}

	/*public void getLocalFileCopyIn(String dir) {
		if (isURL()) {
			String fileName = GenericUtil.getUniqueString() + ".dn";
			getLocalFileCopyIn(dir, fileName);
		}
	}

	public void getLocalFileCopyIn(String dir, String fileName) {
		if (isURL()) {
			// String fileName = GenericUtil.getUniqueString() + ".dn";
			try {
				logger.info("making local file copy of " + getData()
						+ " in directory " + dir);
				File localFile = new File(dir, fileName);
				download(localFile);
				// download(new File(dir, fileName));
				setData(Sorcer.getScratchURL(new File(dir, fileName)));
				logger.info("set context node URL to: " + getData());
				StringBuffer cmd = new StringBuffer("chmod +777 ")
						.append(localFile);
				Process p = Runtime.getRuntime().execEnt(cmd.toString());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ContextNodeException e) {
				e.printStackTrace();
			}
		}
	}

	public void getLocalFileCopy() {
		getLocalFileCopyIn(""+Sorcer.getDataDir());
	}*/

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
