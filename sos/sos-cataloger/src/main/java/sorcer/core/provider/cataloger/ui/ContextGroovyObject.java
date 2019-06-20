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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import groovy.lang.GroovyShell;

/**
 * This class is used to represent a Groovy Object stored in a context. A special class was created
 * because compilation issues can occur if the Groovy libraries are not linked properly. This
 * class also includes methods to compute the Groovy expression.
 * 
 * More information can be found at: http://groovy.codehaus.org/
 * 
 * Providers that utilize the execute methods of this object must add:
 * 
 *  ${contextReturn.separator}${iGrid.home}/common/groovy.jar to the sorcer-jars section of their run.xml
 *  
 *  See groovy.doc under the notes section for more information.
 * 
 * @author Greg McChesney
 *
 */
public class ContextGroovyObject implements Serializable
{

	/**
	 * serialVersionUID used for serialization 
	 */
	private static final long serialVersionUID = 1231231231231293439L;
	
	/**
	 * the expression to be evaluated
	 */
	private String expression;
	
	/**
	 * Constructor for ContextGroovyObject used to create it with an expression
	 * @param expression String representing what needs to evaluated in Groovy
	 */
	public ContextGroovyObject(String expression)
	{
		this.expression=expression;
	}
	
	/** 
	 * This returns the evaluated eval of the Groovy expression specified in the constructor.
	 * In the event the expression was not valid an error message is displayed and the method returns null.
	 * 
	 * @return Object evaluated from expression
	 * @throws Exception 
	 * @throws Exception if there is a problem
	 */
	public Object getValue() throws Exception
	{
		if( expression == "" ) 
			return null;
		try
		{
			GroovyShell shell=new GroovyShell();
		
			try 
			{
				synchronized(shell) 
				{
					return shell.evaluate(expression);
				}
				
			} 
			catch(RuntimeException e) 
			{
				System.out.println("Error Occurred in Groovy Shell-execute! With Attributes"+e.getMessage());
				throw new Exception("Groovy Parsing Error: "+e.getMessage());
			}
		}
		catch(Throwable e) //catch big issues with groovy shell EX: shell not found
		{
			String message="";
			if(e.getMessage().startsWith("Groovy Parsing")) //used to rethrow runtime issues
			{
				message=e.getMessage();
			}
			else //issue with the shell it self.
			{
				message="Groovy support missing-add ${contextReturn.separator}${iGrid.home}/common/groovy.jar to sorcer-jars in your run.xml";
				System.out.println("***ERROR-Current provider was not run with Groovy support ***");
				System.out.println("This can be fixed by adding ${contextReturn.separator}${iGrid.home}/common/groovy.jar to sorcer-jars in your run.xml");
				System.out.println("More information on Groovy support is available under notes/groovy.doc");
			}
			throw new Exception(message);
		}
	}
	
	/** 
	 * This returns the evaluated eval of the Groovy expression specified in the constructor.
	 * This version allows the user to register 
	 * In the event the expression was not valid an error message is displayed and the method returns null.
	 * 
	 *
	 * 
	 * @param attributes HashMap<String,Object> of items to register with the Groovy interpreter. The key is the
	 * 						key of the variable in Groovy and the Object is its eval.
	 * @return Object evaluated from expression, null if expression is empty.
	 * @throws Exception if there is a problem
	 */
	public Object getValue(HashMap<String,Object> attributes ) throws Exception
	{
		if( expression == "" ) 
			return null;
		try
		{
			GroovyShell shell=new GroovyShell();
		
			try 
			{
				synchronized(shell) 
				{
					Set set = attributes.keySet();
					String key="";
					for (Iterator it = set.iterator(); it.hasNext();)  //register each variable
					{
						key=(String)it.next();
						System.out.println("key "+key+" attribute "+attributes.get(key));
						shell.setVariable(key,attributes.get(key));
					}
					return shell.evaluate(expression);
				}
				
			} 
			catch(RuntimeException e) 
			{
				System.out.println("Error Occurred in Groovy Shell-execute! With Attributes"+e.getMessage());
				throw new Exception("Groovy Parsing Error: "+e.getMessage());
			}
		}
		catch(Throwable e) //catch big issues with groovy shell EX: shell not found
		{
			String message="";
			if(e.getMessage().startsWith("Groovy Parsing")) //used to rethrow runtime issues
			{
				message=e.getMessage();
			}
			else //issue with the shell it self.
			{
				message="Groovy support missing-add ${contextReturn.separator}${iGrid.home}/common/groovy.jar to sorcer-jars in your run.xml";
				System.out.println("***ERROR-Current provider was not run with Groovy support ***");
				System.out.println("This can be fixed by adding ${contextReturn.separator}${iGrid.home}/common/groovy.jar to sorcer-jars in your run.xml");
				System.out.println("More information on Groovy support is available under notes/groovy.doc");
			}
			throw new Exception(message);
		}
	}
}
