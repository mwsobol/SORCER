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

package sorcer.core.context;

import java.util.ArrayList;
import java.util.List;

import sorcer.core.SorcerConstants;
import sorcer.service.ContextException;
import sorcer.service.IndexedContext;

/**
 * The Array context has an array like access to a service context. Accessing
 * and setting context values is done via integer indices. Implicit context paths
 * describe i-th element of "array" and are used by the following getters and
 * setters (where: v - eval, c - comment, d - description, i - input, o -
 * output) as follows:
 * <ol>
 * <li>the eval of the i-th element: by v(i) and the setter v(i, obj);<br>
 * implicit contextReturn: array/[i]/eval<br>
 * <li>the comment of the i-th element: by vc(i) and the setter vc(i, comment);<br>
 * implicit contextReturn: array/[i]/comment<br>
 * <li>the description of the i-th element: by vd(i) and the setter vd(i, description);<br>
 * implicit contextReturn: array/[i]/description
 * <li>the input eval of the i-th element: by iv(i) and the setter iv(i, obj);<br>
 * implicit contextReturn: array/in/[i]/eval<br>
 * <li>the comment of the i-th element: by ivc(i) and the setter  ivc(i, comment);<br>
 * implicit contextReturn: array/in/[i]/comment<br>
 * <li>the description of the i-th element: by ivd(i) and the setter ivd(i, description);<br>
 * implicit contextReturn: array/in/[i]/description
 * <li>the output eval of the i-th element: by ov(i) and the setter ov(i, obj);<br>
 * implicit contextReturn: array/out/[i]/eval<br>
 *<li>the comment of the i-th element: by ovc(i) and the setter ovc(i, comment);<br>
 * implicit contextReturn: array/out/[i]/comment<br>
 *<li>the description of the i-th element: by ovd(i) and the setter ovd(i, description);<br>
 * implicit contextReturn: array/out/[i]/description
 */
public class ArrayContext extends ServiceContext implements IndexedContext,
		SorcerConstants {

	private static final long serialVersionUID = 108375572414579267L;

	// maximal eval index in a context, excluding comments and descriptions
	protected int maxIndex = 0;

	/**
	 * Initializes a context array with the key 'Array Context'.
	 */
	public ArrayContext() {
		super("Array Context");
	}

	/**
	 * Initializes a context array with the key specified.
	 * 
	 * @param name
	 *            context key
	 */
	public ArrayContext(String name) {
		super(name);
	}

	public ArrayContext(String name, String subjectPath, Object subjectValue) {
		super(name, subjectPath, subjectValue);
	}
	
	/**
	 * Returns the eval at the specified position in this context (v - Value).
	 * 
	 * @param index
	 *            index of eval to return.
	 * @return eval at the specified index in
	 * 
	 * @throws ContextException
	 */
	public Object v(int index) throws ContextException {
		return getValue(vp(index));
	}

	/**
	 * Replaces the eval at the specified position in this context with the
	 * specified eval (v - Value).
	 * 
	 * @param index
	 *            index of eval to replace.
	 * @param value
	 *            eval to be stored at the specified position.
	 * 
	 * @throws ContextException
	 */
	public void v(int index, Object value) throws ContextException {
		String path = vp(index);
		Contexts.putInValue(this, path, value);
		if (index > maxIndex)
			maxIndex = index;
	}

	/**
	 * Returns the input eval at the specified position in this context (iv -
	 * Input Value).
	 * 
	 * @param index
	 *            index of input eval to return.
	 * @return input eval at the specified index in
	 * 
	 * @throws ContextException
	 */
	public Object iv(int index) throws ContextException {
		return getValue(ivp(index));
	}

	/**
	 * Replaces the input eval at the specified position in this context with
	 * the specified eval (iv - Input Value).
	 * 
	 * @param index
	 *            index of input eval to replace.
	 * @param value
	 *            input eval to be stored at the specified position.
	 * 
	 * @throws ContextException
	 */
	public String iv(int index, Object value) throws ContextException {
		String path = ivp(index);
		Contexts.putInValue(this, path, value);
		if (index > maxIndex)
			maxIndex = index;
		return path;
	}

	/**
	 * Returns the output eval at the specified position in this context (ov -
	 * Output Value).
	 * 
	 * @param index
	 *            index of output eval to return.
	 * @return output eval at the specified index in
	 * 
	 * @throws ContextException
	 */
	public Object ov(int index) throws ContextException {
		return getValue(ovp(index));
	}

	/**
	 * Replaces the output eval at the specified position in this context with
	 * the specified eval (ov - Output Value).
	 * 
	 * @param index
	 *            index of output eval to replace.
	 * @param value
	 *            output eval to be stored at the specified position.
	 * 
	 * @throws ContextException
	 */
	public String ov(int index, Object value) throws ContextException {
		String path = ovp(index);
		Contexts.putOutValue(this, path, value);
		if (index > maxIndex)
			maxIndex = index;
		return path;
	}

	/**
	 * Assigns the comment associated directly with the root node at in this
	 * context (c - Comment).
	 * 
	 * @param comment
	 *            a root context node comment
	 * @throws ContextException
	 */
	public void c(String comment) throws ContextException {
		putValue(COMMENT, comment);
	}

	/**
	 * Returns the comment associated directly with the root node at in this
	 * context (c - Comment).
	 * 
	 * @return comment for the root node
	 * 
	 * @throws ContextException
	 */
	public String c() throws ContextException {
		return (String) getValue(COMMENT);
	}

	/**
	 * Returns the comment associated with a eval at the specified position in
	 * this context (c - Comment).
	 * 
	 * @param index
	 *            index of comment to return.
	 * @return comment at the specified index in
	 * 
	 * @throws ContextException
	 */
	public String vc(int index) throws ContextException {
		return (String) getValue(cp(index));
	}

	/**
	 * Assigns the comment associated with an input eval at the specified
	 * position in this context (c - Comment).
	 * 
	 * @param index
	 *            index of comment to replace.
	 * 
	 * @throws ContextException
	 */
	public void vc(int index, String comment) throws ContextException {
		putValue(cp(index), comment);
	}

	/**
	 * Returns the comment associated with an input eval at the specified
	 * position in this context (ivc - Input Value Comment).
	 * 
	 * @param index
	 *            index of comment to return.
	 * @return comment at the specified index in
	 * 
	 * @throws ContextException
	 */
	public String ivc(int index) throws ContextException {
		return (String) getValue(civp(index));
	}

	/**
	 * Assigns the comment associated with an input eval at the specified
	 * position in this context (ivc - Input Value Comment).
	 * 
	 * @param index
	 *            index of comment to replace.
	 * 
	 * @throws ContextException
	 */
	public void ivc(int index, String comment) throws ContextException {
		putValue(civp(index), comment);
	}

	/**
	 * Returns the comment associated with an output eval at the specified
	 * position in this context (ovc - Output Value Comment).
	 * 
	 * @param index
	 *            index of comment to return.
	 * @return comment at the specified index in
	 * 
	 * @throws ContextException
	 */
	public String ovc(int index) throws ContextException {
		return (String) getValue(covp(index));
	}

	/**
	 * Assigns the comment associated with an output eval at the specified
	 * position in this context (ovc - Output Value Comment).
	 * 
	 * @param index
	 *            index of comment to replace.
	 * 
	 * @throws ContextException
	 */
	public void ovc(int index, String comment) throws ContextException {
		putValue(covp(index), comment);
	}

	/**
	 * Assigns the description associated directly with the root node at in this
	 * context (d - Description).
	 * 
	 * @param description
	 *            a root context node description
	 * @throws ContextException
	 */
	public void d(String description) throws ContextException {
		putValue(DESCRIPTION, description);
	}

	/**
	 * Returns the description associated directly with the root node at in this
	 * context (d - Description).
	 * 
	 * @return description for the root node
	 * 
	 * @throws ContextException
	 */
	public String d() throws ContextException {
		return (String) getValue(DESCRIPTION);
	}

	/**
	 * Returns the description associated with a eval at the specified position
	 * in this context (d - Description).
	 * 
	 * @param index
	 *            index of description to return.
	 * @return description at the specified index in
	 * 
	 * @throws ContextException
	 */
	public String vd(int index) throws ContextException {
		return (String) getValue(dp(index));
	}

	/**
	 * Assigns the description associated with a eval at the specified position
	 * in this context (d - Description).
	 * 
	 * @param index
	 *            index of comment to replace.
	 * @param description
	 *            a context node description
	 * @throws ContextException
	 */
	public void vd(int index, String description) throws ContextException {
		putValue(dp(index), description);
	}

	/**
	 * Returns the description associated with an input eval at the specified
	 * position in this context (ivd - Input Value Description).
	 * 
	 * @param index
	 *            index of description to return.
	 * @return description at the specified index in
	 * 
	 * @throws ContextException
	 */
	public String ivd(int index) throws ContextException {
		return (String) getValue(divp(index));
	}

	/**
	 * Assigns the description associated with an input eval at the specified
	 * position in this context (ivd - Input Value Description).
	 * 
	 * @param index
	 *            index of comment to replace.
	 * 
	 * @throws ContextException
	 */
	public void ivd(int index, String description) throws ContextException {
		putValue(divp(index), description);
	}

	/**
	 * Returns the description associated with an output eval at the specified
	 * position in this context (ovd - Output Value Description).
	 * 
	 * @param index
	 *            index of description to return.
	 * @return description at the specified index in
	 * 
	 * @throws ContextException
	 */
	public String ovd(int index) throws ContextException {
		return (String) getValue(dovp(index));
	}

	/**
	 * Assigns the description associated with an output eval at the specified
	 * position in this context (ovd - Output Value Description).
	 * 
	 * @param index
	 *            index of comment to replace.
	 * 
	 * @throws ContextException
	 */
	public void ovd(int index, String description) throws ContextException {
		putValue(dovp(index), description);
	}

	/**
	 * Returns a list of input context values marked as data input.
	 * 
	 * @return a list of input values of this context
	 * @throws ContextException
	 * @throws ContextException
	 */
	@Override
	public List<?> getInValues() throws ContextException {
		String[] inpaths = getSortedInPaths();
		List array = new ArrayList(inpaths.length);
		for (Object path : inpaths)
			try {
				array.add(getValue((String) path));
			} catch (ContextException e) {
				throw new ContextException(e);
			}

		return array;
	}

	// Returns a list of input values sorted by index using insertion sort
	public String[] getSortedInPaths() throws ContextException {
		// find all input contextReturn
		String inAssoc = DIRECTION + SorcerConstants.APS + DA_IN;
		String inoutAssoc = DIRECTION + SorcerConstants.APS + DA_INOUT;
		String[] inPaths = Contexts.getMarkedPaths(this, inAssoc);
		String[] inoutPaths = Contexts.getMarkedPaths(this, inoutAssoc);
		int tally = inPaths.length + inoutPaths.length;
		String[] list = new String[tally];
		if (inPaths != null)
			for (int i = 0; i < inPaths.length; i++)
				list[i] = inPaths[i];
		if (inoutPaths != null)
			for (int i = inPaths.length; i < inoutPaths.length; i++)
				list[i] = inoutPaths[i];
		// insertion sort
		int in, out;

		for (out = 1; out < tally; out++) // out is dividing line
		{
			String temp = list[out]; // remove marked impl
			in = out; // start shifts at out
			// until one is smaller
			while (in > 0 && getIndex((String) list[in - 1]) >= getIndex(temp)) {
				list[in] = list[in - 1]; // shift impl right,
				--in; // go left one position
			}
			// insert marked impl
			list[in] = temp;
		} // end for
		return list;
	}

	// utility methods defining paths of this context
	public static String civp(int index) {
		return (ii(index) + CPS + COMMENT).intern();
	}

	public static String covp(int index) {
		return (oi(index) + CPS + COMMENT).intern();
	}

	public static String divp(int index) {
		return (ii(index) + CPS + DESCRIPTION).intern();
	}

	public static String dovp(int index) {
		return (oi(index) + CPS + DESCRIPTION).intern();
	}

	public static String ivp(int index) {
		return (ii(index) + CPS + VAL).intern();
	}

	public static String ovp(int index) {
		return (oi(index) + CPS + VAL).intern();
	}

	public static String ii(int index) {
		return (IN + CPS + '[' + index + ']').intern();
	}

	public static String oi(int index) {
		return (OUT + CPS + '[' + index + ']').intern();
	}

	public static String vp(int index) {
		return ("[" + index + ']' + CPS + VAL).intern();
	}

	public static String cp(int index) {
		return ("[" + index + ']' + CPS + COMMENT).intern();
	}

	public static String dp(int index) {
		return ("[" + index + ']' + CPS + DESCRIPTION).intern();
	}

	public int getMaxIndex() {
		return maxIndex;
	}

	public String getPath(String selector, int index) {
		if (selector.equals("iv"))
			return ivp(index);
		else if (selector.equals("ov"))
			return ovp(index);
		if (selector.equals("v"))
			return vp(index);

		return null;
	}

	/**
	 * Return an index of this indexed context contextReturn.
	 * 
	 * @param path
	 *            IndexedContext context contextReturn
	 * @return an index of the given service context contextReturn
	 */
	public int getIndex(String path) {
		int i1 = path.indexOf('[');
		if (i1 >= 0) {
			int i2 = path.indexOf(']');
			return Integer.parseInt(path.substring(i1 + 1, i2));
		}
		return -1;
	}

	// constants used for this context
	public final static String IDEX = "index";

	public final static String VAL = "eval";

	public final static String VAL_ = "eval" + CPS;

	public final static String IN = "input";

	public final static String IN_ = "input" + CPS;

	public final static String OUT = "output";

	public final static String OUT_ = "output" + CPS;

	public final static String COMMENT = "comment";

	public final static String DESCRIPTION = "description";

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.context.IndexedContext#execute(int)
	 */
	@Override
	public Object getValue(int i) throws ContextException {
		return v(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.context.IndexedContext#putValue(int, java.lang.Object)
	 */
	@Override
	public Object putValue(int i, Object value) throws ContextException {
		v(i, value);
		return value;
	}

}