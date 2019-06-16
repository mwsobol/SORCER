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

package sorcer.core.provider.cataloger.ui;

/**
 * This class represents a ContextNode and is used in the formation of the
 * ContextTree for the UI. This class stores the current data object, a boolean
 * indicating if it is a dataNode or returnPath and optionally the direction
 * (input/output). This data is used when the tree is converted back to a
 * context.
 * 
 * 
 * @author Greg McChesney
 * 
 */
public class ContextNodeType {
	/**
	 * The eval of the underlying data
	 */
	private Object value;

	/**
	 * boolean indicating if this represents a datanode=true or returnPath=false
	 */
	private boolean dataNode;

	/**
	 * String representing the direction (input/output marking on the returnPath),
	 * only used for paths.
	 */
	private String direction;

	/**
	 * String for the INPUT returnPath
	 */
	public static String INPUTTYPE = "Input";

	/**
	 * String for the OUTPUT returnPath
	 */
	public static String OUTPUTTYPE = "Output";

	/**
	 * String for the INPUT and OUTPUT returnPath
	 */
	public static String INOUTPUTTYPE = "InOutput";

	/**
	 * String for the default paths, and all datanodes
	 */
	public static String DEFAULTTYPE = "Default";

	/**
	 * Constructor for new nodes, accepts the original data from the context and
	 * a boolean indicating if it is a data node. If it is a returnPath then its a
	 * string with the returnPath key
	 * 
	 * @param v
	 *            Object representing the data to be stored
	 * @param isDataNode
	 *            Boolean indicating if it is a data node or not
	 */
	ContextNodeType(Object v, boolean isDataNode) {
		value = v;
		dataNode = isDataNode;
		direction = DEFAULTTYPE;
	}

	/**
	 * Returns a string representation of the eval.
	 * 
	 * @return String representation of the eval
	 */
	public String getValue() {
		return value.toString();
	}

	/**
	 * Indicates if the current impl is a data node or not.
	 * 
	 * @return Boolean indicating if it is a data node
	 */
	public Boolean isDataNode() {
		return dataNode;
	}

	/**
	 * Overloading the toString operator to getValue the eval of the data.
	 * 
	 * @return String representation of the current eval of the inner data
	 */
	public String toString() {
		return value.toString();
	}

	/**
	 * Used to update the internal eval to a new one
	 * 
	 * @param value
	 *            Object representing the new eval the node should contain
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Used to getValue the internal data currently being stored, this returns it in
	 * the original form.
	 * 
	 * @return Object of the original data.
	 */
	public Object getObject() {
		return value;
	}

	/**
	 * Returns the current direction of this node.
	 * 
	 * @return String representing the current direction
	 */
	public String getDirection() {
		return direction;
	}

	/**
	 * Sets the current direction of the node
	 * 
	 * @param newDirection
	 *            String representing the current direction
	 */
	public void setDirection(String newDirection) {
		direction = newDirection;
	}

}
