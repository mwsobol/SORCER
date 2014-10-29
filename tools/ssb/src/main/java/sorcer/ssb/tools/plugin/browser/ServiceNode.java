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

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.lookup.entry.Name;
import net.jini.lookup.entry.ServiceInfo;

public class ServiceNode{
	
	ServiceItem _serviceItem;
	String _strValue;
	boolean _isLus;
	Object _userObject;
	Entry [] _atts;
	
	ServiceNode(ServiceItem item){
		
		try{
			
			_serviceItem=item;
			_atts=item.attributeSets;
			if(_serviceItem.service instanceof ServiceRegistrar){
				ServiceRegistrar reggie=(ServiceRegistrar)_serviceItem.service;
				LookupLocator ll=reggie.getLocator();
				_strValue=ll.getHost()+":"+ll.getPort();
			}else{
				setName();
				//_serviceItem.service.toString();
			}
		}catch(Exception ex){
			ex.printStackTrace();
			_strValue=_serviceItem.service.toString();
		}
	}
	void setName(){
		_strValue=getName(_atts);
		if(_strValue==null){
			_strValue=_serviceItem.service.getClass().getName();//+" - "+
			_strValue=TreeRenderer.getJiniName(_serviceItem.service,_strValue);
		}
	}
	String getName(){
		return _strValue;
	}
	void updateServiceItem(ServiceItem item){
		_serviceItem=item;
	}
	void updateServiceItem(ServiceItem item,boolean updateAtts){
		_serviceItem=item;
		if(updateAtts){
			_atts=_serviceItem.attributeSets;
			setName();
		}
	}
	boolean sameServiceID(Object sid){
		return sid.equals(_serviceItem.serviceID);
	}
	Object getProxy(){
		return _serviceItem.service;
	}
	ServiceID getServiceID(){
		return _serviceItem.serviceID;
	}
	void markAsLus(){
		_isLus=true;
	}
	void markAsService(){
		_isLus=false;
	}
	boolean isLus(){
		return _isLus;
	}
	Entry [] getLookupAttributes(){
		return _atts;
	}
	ServiceItem getServiceItem(){
		return _serviceItem;
	}
	
	public void setName(String name){
		_strValue=name;
	}
	public String toString(){
		
		return _strValue;
	}
	public void setUserObject(Object obj){
		_userObject=obj;
	}
	public Object getUserObject(){
		return _userObject;
	}
	public boolean ping(){
		try{
			if(_serviceItem.service instanceof ServiceRegistrar){
				ServiceRegistrar reggie=(ServiceRegistrar)_serviceItem.service;
				reggie.getLocator();
				return true;
			}else{
				return pingAdmin();
			}
			
		}catch(Exception ex){
			//ex.printStackTrace();
			//System.out.println("ping() retuning false");
			return false;
		}
		
	}
	public boolean pingAdmin(){
		try{
			Object proxy=getProxy();
			if(proxy instanceof Administrable){
				
				Administrable admin=(Administrable)proxy;
				Object adminProxy=admin.getAdmin();
				if(adminProxy instanceof JoinAdmin){
					_atts=((JoinAdmin)adminProxy).getLookupAttributes();
					_serviceItem=new ServiceItem(_serviceItem.serviceID,_serviceItem.service,_atts);
					setName();
				}
				
			}
			return true;
		}catch(Exception ex){
			
			return false;
		}
		
	}
	private String getName(Entry [] atts){
		//make sure we use the Name.name attribute in preference
		for(int i=0;atts!=null && i<atts.length;i++){
			if(atts[i] instanceof Name){
				String name=((Name)atts[i]).name;
				if(name!=null && name.length()>0){
					return name;	
				}
				
			}
		}
		for(int i=0;atts!=null && i<atts.length;i++){
			if(atts[i] instanceof ServiceInfo){
				String name=((ServiceInfo)atts[i]).name;
				if(name!=null && name.length()>0){
					return name;	
				}
				
			}
		}
		return null;
	}
}
