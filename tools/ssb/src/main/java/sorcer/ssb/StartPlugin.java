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

package sorcer.ssb;

import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;

import sorcer.ssb.tools.plugin.browser.ServiceBrowser;

public class StartPlugin {

	public static void main(final String[] args) {
		try {

			/*
			 * String [] cArgs={"D:/sorcer/service-browser/browser.config"};
			 * final Configuration config =
			 * ConfigurationProvider.getInstance(cArgs);
			 * 
			 * 
			 * sorcer.ssb.tools.plugin.browser.ServiceBrowserUI._exporter=(Exporter
			 * ) config.getEntry( "com.ssb.tools.browser", "exporter",
			 * Exporter.class, new
			 * BasicJeriExporter(TcpServerEndpoint.getInstance(0), new
			 * BasicILFactory()));
			 * 
			 * 
			 * LoginContext loginContext = (LoginContext) config.getEntry(
			 * "sorcer.ssb.tools.browser", "loginContext", LoginContext.class,
			 * null); if (loginContext == null) {
			 * sorcer.ssb.tools.plugin.browser.ServiceBrowser.start(args); }
			 * else { loginContext.login(); Subject.doAsPrivileged(
			 * loginContext.getSubject(), new PrivilegedExceptionAction() {
			 * public Object run() throws Exception {
			 * sorcer.ssb.tools.plugin.browser.ServiceBrowser.start(args);
			 * return null; } }, null); }
			 */
			// args[0]==SSB home directory

			ServiceBrowser.start(args, true, null);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	public static Properties getProperties() {
		Properties props = new Properties();
		props.setProperty("plugin.name", ServiceBrowser.TITLE);
		// props.setProperty("admin.compatible","true");
		try {
			URL iconUrl = StartPlugin.class.getClassLoader().getResource(
					"rt-images/sorcer.png");
			props.put("plugin.icon", new ImageIcon(iconUrl));

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return props;
	}

	/**
	 * Initialize your plugin with the IncaXEnvironment
	 * 
	 */
	/*
	 * public void setEnvironment(SSBEnvironment env) throws PluginException{
	 * //keep hold of the environment object ssbEnv=env; //set your plugins icon
	 * try{ URL
	 * iconUrl=getClass().getClassLoader().getResource("rt-images/lus.gif");
	 * ImageIcon myIcon=new ImageIcon(iconUrl); ssbEnv.setPluginIcon(myIcon);
	 * 
	 * }catch(Exception ex){ throw new PluginException(ex); }
	 * 
	 * JMenuItem myItem=new JMenuItem("Lookup service browser...");
	 * myItem.addActionListener( new ActionListener(){ public void
	 * actionPerformed(ActionEvent evt){
	 * 
	 * showWindow();
	 * 
	 * } }); //add your menus to the IDE JMenuItem [] items=new
	 * JMenuItem[]{myItem}; ssbEnv.setToolsMenuItems(items); } //Refernce to the
	 * SSB Environment private SSBXEnvironment ssbEnv; //implementation details
	 * 
	 * private void showWindow(){ try{ //add a window to the IDE JPanel
	 * ui=com.sorcer.ssb.tools.plugin.browser.ServiceBrowser.createUI();
	 * ssbEnv.addView("Lookup service browser",ui);
	 * 
	 * }catch(Exception ex){ ex.printStackTrace();
	 * JOptionPane.showMessageDialog(null,ex); }
	 * 
	 * }
	 */
}
