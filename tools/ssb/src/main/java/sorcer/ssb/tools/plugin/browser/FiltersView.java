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

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import sorcer.ssb.browser.api.SSBrowserFilter;
import sorcer.ssb.jini.studio.CodeServer;


public class FiltersView extends JPanel{
	private JTabbedPane _tp=new JTabbedPane();
	private JEditorPane _interfaces=new JEditorPane();
	private JEditorPane _groups=new JEditorPane();
	private JEditorPane _lookupLocators=new JEditorPane();
	private JEditorPane _nameAttributes=new JEditorPane();
	private JList _plugins=new JList();
	//	private String FNAME="filters.ser";
	///	private String _browserHome;
	private FilterRegistry _reg=new FilterRegistry();
		
	public FiltersView(){
		setLayout(new BorderLayout());
		add(_tp,BorderLayout.CENTER);
		//force initialization of images
		new TreeRenderer();
		
		_plugins.setModel(new DefaultListModel());
		
		
		_tp.add("Interfaces",new JScrollPane(_interfaces));
		_tp.add("Groups",new JScrollPane(_groups));
		_tp.add("Lookup locators",new JScrollPane(_lookupLocators));
		_tp.add("Names",new JScrollPane(_nameAttributes));
		_tp.add("Plugins",new JScrollPane(_plugins));
		
		_tp.setIconAt(0,TreeRenderer._serviceIcon);
		_tp.setIconAt(1,TreeRenderer._lusIcon);
		_tp.setIconAt(2,TreeRenderer._sidIcon);
		_tp.setIconAt(3,TreeRenderer._serviceIcon);
		_tp.setIconAt(4,TreeRenderer._jarIcon);
		
		setDefaultText();
		
		_reg.init(new File(ServiceBrowserConfig.BROWSER_HOME+"/plugins"));
		
		DefaultListModel model=(DefaultListModel)_plugins.getModel();
		ArrayList filterClasses=_reg.getFilters();
		int n=filterClasses.size();
		for(int i=0;i<n;i++){
			//System.out.println("----- "+filterClasses.get(i));
			SSBrowserFilter bf=(SSBrowserFilter)filterClasses.get(i);
			model.addElement(bf.getDisplayName());
		}
		/*
		try{
		URL url=getClass().getProtectionDomain().getCodeSource().getLocation();
		String path=url.getPath();
		if(path.endsWith(".jar")){
		path=path.substring(0,path.lastIndexOf("/")+1);
		}else{
		path+="/";
		}
		_browserHome=path;
		
		//FNAME=path+FNAME;
		
		//System.out.println("Filter store="+FNAME);
		
		}catch(Exception ex){
		
		}
		*/
	}
	public String [] getAllText(){
		return new String[]{_interfaces.getText(),_groups.getText(),_lookupLocators.getText(),_nameAttributes.getText()};
	}
	public void restoreText(String[] t){
		_interfaces.setText(t[0]);
		_groups.setText(t[1]);
		_lookupLocators.setText(t[2]);
		_nameAttributes.setText(t[3]);
		
		
	}
	public void setDefaultText(){
		String infText="//   Type in the full name of the service's interface classes\n"+
		"//   that you want to filter on for example\n"+
		"//   net.jini.event.EventMailbox\n"+
		"//   net.jini.space.JavaSpace\n\n"+
		"//   Use one line for each item\n";
		
		_interfaces.setText(infText);
		String grpText="//  Lookup service filters\n"+
		"//  Use PUBLIC for the public group\n"+
		"//  ALL_GROUPS for all groups\n"+
		"//   Use one line for each item\n\n"+
		"ALL_GROUPS";
		
		_groups.setText(grpText);
		String locText="//  Specify your LookupLocators as full Jini URLS\n"+
		"//  For example\n"+
		"//  jini://dev.incax.com\n\n"+
		"//   Use one line for each item\n//  Please note: This filter overrides all other filters\n";
		
		_lookupLocators.setText(locText);
		
		String nameText="//   Type in the Name attributes of the services\n"+
		"//   that you want to filter on for example\n"+
		"//   My JavaSpace\n"+
		"//   Peters JavaSpace\n\n"+
		"//   Use one line for each item\n//  Please note: This filter overrides the Interfaces filter\n";
		
		_nameAttributes.setText(nameText);
		ListSelectionModel lsm=_plugins.getSelectionModel();
		lsm.clearSelection();
	}
	public String [] getInterfaces(){
		return parse(_interfaces);
	}
	public String [] getGroups(){
		return parse(_groups);
	}
	public String [] getLookupLocators(){
		return parse(_lookupLocators);
	}
	public String [] getNameFilters(){
		return parse(_nameAttributes);
	}
	public SSBrowserFilter getPlugin(){
		int sel=_plugins.getSelectedIndex();
		if(sel==-1){
			return null;
		}
		DefaultListModel model=(DefaultListModel)_plugins.getModel();
		ArrayList filterClasses=_reg.getFilters();
		return (SSBrowserFilter)filterClasses.get(sel);
	}
	private String [] parse(JEditorPane ep){
		ArrayList list=new ArrayList();
		String text=ep.getText();
		StringTokenizer tok=new StringTokenizer(text,"\n");
		while(tok.hasMoreTokens()){
			String ln=tok.nextToken().trim();
			if(ln.startsWith("//")==false && ln.length()>0){
				list.add(ln);
			}
		}
		String [] ret=new String[list.size()];
		list.toArray(ret);
		return ret;
	}
	public String toString(){
		StringBuffer buf=new StringBuffer();
		String [] t=getAllText();
		for(int i=0;i<t.length;i++){
			buf.append(t[i]+"\n");
		}
		return buf.toString();
	}
}
