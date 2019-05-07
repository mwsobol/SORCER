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
import java.awt.Font;
import java.lang.reflect.Method;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class PropertiesView extends JPanel{

	public PropertiesView(DefaultMutableTreeNode pNode){
		
		setLayout(new BorderLayout());
		//boolean isMethodsNode=pNode.toString().equals("Methods");
		int nKids=pNode.getChildCount();
		Object [] data=new Object[nKids];
		for(int i=0;i<nKids;i++){
			DefaultMutableTreeNode kid=(DefaultMutableTreeNode)pNode.getChildAt(i);
			Object uo=kid.getUserObject();
			if(uo instanceof PropertiesNode){
				PropertiesNode pn=(PropertiesNode)uo;
				Object addData=pn.getAdditionalData();
				if(addData!=null){
					if(addData instanceof Method){
						data[i]=LusTree.parseMethod((Method)addData);	
					}else{
						data[i]=addData.toString();
					}
				}else{
					data[i]=pn.toString();
				}
			}else{
				data[i]=uo.toString();
			}
		}
		
		JLabel title=new JLabel(pNode.toString());
		Font font=title.getFont();
		title.setFont(new Font(font.getFamily(),Font.BOLD,font.getSize()+1));
		
		Object userObject=pNode.getUserObject();
        if(userObject instanceof PropertiesNode){
			TreeRenderer.setIconForProps(title,(PropertiesNode)userObject);
			
			//System.out.println("###PropertiesNode "+userObject);
		}else{
			title.setIcon(TreeRenderer._sidIcon);
		}
		
		JList list=new JList(data);
		if(!pNode.isRoot()){
		/*
			StringBuffer buf=new StringBuffer();
			TreeNode [] tPath=pNode.getPath();
			for(int i=1;i<tPath.length;i++){
				String str=tPath[i].toString();
				buf.append(str);
				if(i<tPath.length-1 && !str.endsWith("/")){
					buf.append("/");
				}
			}
			*/
			JLabel status=new JLabel(getPath(pNode));//buf.toString());
			add( status, BorderLayout.SOUTH);
		}
		add( title, BorderLayout.NORTH);
		add( new JScrollPane(list), BorderLayout.CENTER);
		
		
	}
	static String getPath(DefaultMutableTreeNode pNode){
		StringBuffer buf=new StringBuffer();
		TreeNode [] tPath=pNode.getPath();
		for(int i=1;i<tPath.length;i++){
			String str=tPath[i].toString();
			buf.append(str);
			if(i<tPath.length-1 && !str.endsWith("/")){
				buf.append("/");
			}
		}
		return buf.toString();
		
	}
}
