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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.JComponent;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;

public class GlyphView extends JComponent{
	
	private ArrayList _glyphs=new ArrayList();				
	private Glyph _selected;
	private Glyph _lastSelected;	
	private Rectangle _viewRect;
	public static final Color LAYER_COLOR=new Color(218,227,252);
	private ServiceBrowserUI _browser;
	
	public GlyphView(ServiceBrowserUI browser){
		_browser=browser;
		
		addMouseMotionListener( new MouseMotionListener(){
			public void mouseMoved(MouseEvent evt){
				Point p=evt.getPoint();
				processPoint(p);
				//System.out.println(evt);
			}
			public void mouseDragged(MouseEvent evt){
				
			}
			
		}); 
		addMouseListener( new MouseAdapter(){
			public void mouseClicked(MouseEvent evt){
				if(evt.getClickCount()!=2){
					return;
				}
				Point p=evt.getPoint();
				Glyph glyph=(Glyph)_glyphs.get(0);
				//_viewRect=glyph.getBounds();
				if(glyph.containsPoint(p)){
					ServiceNode sn=(ServiceNode)glyph.getUserObject();
					ServiceItem si=sn.getServiceItem();
					_browser.iconDoubleClick(si);
					
				}else{
					ArrayList kids=glyph.getKids();
					int ng=kids.size();
					for(int i=0;i<ng;i++){
						Glyph kg=(Glyph)kids.get(i);
						if(kg.containsPoint(p)){
							Object uo=kg.getUserObject();
							if(uo instanceof ServiceNode){
							
								ServiceNode sn=(ServiceNode)uo;
								ServiceItem si=sn.getServiceItem();
								_browser.iconDoubleClick(si);
							}
						}
					}
				}
			}
		});
	}
		
	public void paint(Graphics g){
	
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			       
		g2.setPaint(Color.white);
		Rectangle r=getBounds();
		g.fillRect(r.x,r.y,r.width,r.height);
		//make it a sq
		int dim=r.width<r.height?r.width:r.height;
		dim-=100;
		int xpos=xpos=(r.width/2)-(dim/2);//r.x+50;
		
		Rectangle newBounds=new Rectangle(xpos,r.y+50,dim,dim);
		
		int ng=_glyphs.size();
		//special case if there's just one glyph
		
		for(int i=0;i<ng;i++){
			Glyph glyph=(Glyph)_glyphs.get(i);
			glyph.setBounds(newBounds);
			glyph.draw(g);
		}
		if(_selected!=null){
			//show a layer
			_selected.highlight(g);
		
			
			Font font=g.getFont();
			FontMetrics fm=g.getFontMetrics();
			
			ArrayList str=new ArrayList(); 
			//add the service name
			String label=_selected.getText();
			str.add(label);
			int maxWidth=fm.stringWidth(label+"  ");
			
			//if(!ServiceBrowser.PROPS_MODE){
				//ServiceItem si=(ServiceItem)_selected.getUserObject();
				//Entry [] atts=si.attributeSets;
				//Object service=si.service;		
				//modified v4.5 [ix-02]
				ServiceNode si=(ServiceNode)_selected.getUserObject();
				Entry [] atts=si.getLookupAttributes();
				
				Object service=si.getServiceItem().service;
				
				if(service!=null && service instanceof ServiceRegistrar){
					//then get the member groups
					ServiceRegistrar reggie=(ServiceRegistrar)service;
					try{
						String [] grps=reggie.getGroups();
						for(int i=0;grps!=null && i<grps.length;i++){
							if(grps[i]!=null && grps[i].length()==0){
								str.add("PUBLIC");
							}else{
								str.add(grps[i]);
							}
						}	
					}catch(Exception ex){
						//ignore
					}
					
					
				}
				if(!SorcerServiceBrowser.PROPS_MODE){
						
				for(int i=0;i<atts.length;i++){
					
					Field [] field=atts[i].getClass().getFields();
					for(int j=0;j<field.length;j++){
						label=field[j].getName();
						
						try{
							label+="="+field[j].get(atts[i]);
						}catch(Exception ex){
							label+="= ???";
						}
						//g.drawString(label,x,y);
						//y+=h+2;
						str.add(label);
						int strWidth=fm.stringWidth(label);
						maxWidth=maxWidth<strWidth?strWidth:maxWidth;	
					}
				
				}
			}
			//calculst rectangle size;
			int h=fm.getHeight();
			int rectWidth=maxWidth+10;
			int rectHeight=(str.size()*(h+2))+10;
			Rectangle gr=_selected.getPreferredRect();
			
			int gcx=gr.x+(gr.width/2);
			int gcy=gr.y+(gr.height/2);
			//can we fit to the right?
			Dimension viewSize=getSize();
			while(gcx+rectWidth>viewSize.width){
				gcx--;
			}
			while(gcy+rectHeight>viewSize.height){
				gcy--;
			}
			
			//g.setColor(LAYER_COLOR);
			//g.fillRect(gcx,gcy,rectWidth,rectHeight);
			GradientPaint bluetowhite = 
				new GradientPaint(gcx,gcy,LAYER_COLOR,gcx+rectWidth,gcy+rectHeight,Color.white);
        	g2.setPaint(bluetowhite);
	        g2.fill (new Rectangle2D.Double(gcx,gcy,rectWidth,rectHeight));
			g.setColor(Color.black);
			int nstr=str.size();
			int x=gcx+5;
			int y=gcy+10;
			
			for(int i=0;i<nstr;i++){
				
				if(i==0){
					//draw line under name
					g.setFont(new Font(font.getFamily(),Font.BOLD,font.getSize()));
					label=(String)str.get(i);
					g.drawString(label,x,y);
					fm=g.getFontMetrics();
					int strWidth=fm.stringWidth(label);
					g.drawLine(x,y+1,x+strWidth,y+1);
					g.setFont(new Font(font.getFamily(),Font.PLAIN,font.getSize()));
				}else{
					g.drawString((String)str.get(i),x,y);
				}
				y+=h+2;
			}
			
		}
	}
	
	public void add(Glyph obj){
		_glyphs.add(obj);
	}
	private void processPoint(Point p){
		//there's only one
		_lastSelected=_selected;
		if(!SorcerServiceBrowser.PROPS_MODE){
			//clear if not in props mode
			_selected=null;
		}
		
		Glyph glyph=(Glyph)_glyphs.get(0);
		//_viewRect=glyph.getBounds();
		if(glyph.containsPoint(p)){
			_selected=glyph;
		}else{
			ArrayList kids=glyph.getKids();
			int ng=kids.size();
			for(int i=0;i<ng;i++){
				Glyph kg=(Glyph)kids.get(i);
				if(kg.containsPoint(p)){
					_selected=kg;		
				}
			}
		}
		if(_selected!=_lastSelected){
			repaint();
			if(_selected!=null){
				
				//ServiceItem si=(ServiceItem)_selected.getUserObject();
				//modified v4.5 [ix-02]
				ServiceNode si=(ServiceNode)_selected.getUserObject();
				Entry [] atts=si.getLookupAttributes();
				//Entry [] atts=si.attributeSets;
				_browser.showProps(atts);
			}
		}
	}
}
