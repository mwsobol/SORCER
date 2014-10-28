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

package sorcer.ssb.tools.plugin.browser;

public class PropertiesNode{
	private Object _data;
	private int _type;
	private Object _additionalData;
	private Object _proxy;
	
	public static final int ENTRY_CLASS=0;
	public static final int ENTRY_FIELD=1;
	public static final int SERVICE_ID=2;
	public static final int INTERFACE=3;
	public static final int METHOD=4;
			
	PropertiesNode(int type,Object data){
		_type=type;
		_data=data;	
	}	
	int getType(){
		return _type;
	}
	public String toString(){
		return _data.toString();
	}
	void setAdditionalData(Object data){
		_additionalData=data;
	}
	Object getAdditionalData(){
		return _additionalData;
	}
	/**
	* getProxy
	* @return Object
	*/
	public Object getProxy(){
		return _proxy;
	}
	/**
	* setProxy
	* @param _proxy
	*/
	public void setProxy(Object _proxy){
		this._proxy=_proxy;
	}

}
