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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;

public class Glyph{

	final static float dash1[] = {5.0f};
    final static BasicStroke dashed = new BasicStroke(1.0f, 
                                                      BasicStroke.CAP_BUTT, 
                                                      BasicStroke.JOIN_MITER, 
                                                      10.0f, dash1, 0.0f);

	final static BasicStroke normal = new BasicStroke();
	static final Color SERVICE_COLOR=new Color(68, 126, 183);  
	static final Color LUS_COLOR=new Color(68, 150, 183);  
	private Image _image;
	private Rectangle _rect;
	private String _text;
	private ArrayList _kids=new ArrayList();
	private ArrayList _points;
	private final int GLYPH_SIZE=80;
	private Object _userObject;
	private boolean _isRoot;
	
	public Glyph(String text,Image image){
		this(text,image,null);	
	}
	public Glyph(String text,Image image,Rectangle rect){
		_text=text;
		_image=image;
		_rect=rect;
	}
	public void setAsRoot(){
		_isRoot=true;
	}
	public boolean isRoot(){
		return _isRoot;
	}
	public Rectangle getPreferredRect(){
		if(!_isRoot){
			return _rect;
		}else{
			//modify for smaller bounds
			double centreX=((double)_rect.width/2);
			double centreY=((double)_rect.height/2);
			int lusDim=50;
			int x=_rect.x+(int)centreX-lusDim;
			int y=_rect.y+(int)centreY-lusDim;
			return new Rectangle(x,y,lusDim*2,lusDim*2);
		}
	}
	public void setUserObject(Object obj){
		_userObject=obj;
	}
	public Object getUserObject(){
		return _userObject;
	}
	public ArrayList getKids(){
		return _kids;
	}
	public void setBounds(Rectangle rect){
		_rect=rect;
	}
	public Rectangle getBounds(){
		return _rect;
	}
	public String getText(){
		return _text;
	}
	public void addChild(Glyph kid){
		_kids.add(kid);
	}
	public void removeAll(){
		_kids=new ArrayList();
	}
	
	public boolean containsPoint(Point p){
		if (getPreferredRect() != null)
			return getPreferredRect().contains(p);
		else
			return false;
	}
	
	boolean overlaps(Rectangle rect){
		int ng=_kids.size();
		for(int i=0;i<ng;i++){
			Glyph kid=(Glyph)_kids.get(i);
			if(kid._rect!=null){
				if(rect.intersects(kid._rect)){
					return true;
				}
			}
		}
		return false;
	}
	//flsu out child rects before repaint
	private void reset(){
		int ng=_kids.size();
		for(int i=0;i<ng;i++){
			Glyph kid=(Glyph)_kids.get(i);
			kid._rect=null;
			
		}
	}
	private int countAvailableSlots(int dim){
		
		double centreX=((double)_rect.width/2);
		double centreY=((double)_rect.height/2);
		int cx=_rect.x+(int)centreX;
		int cy=_rect.y+(int)centreY;
				
       	ArrayList slots=new ArrayList();
       	int nPoints=_points.size();
       
		for(int i=0;i<nPoints;i++){
			Point p=(Point)_points.get(i);
			//adjust point
			int x=p.x+cx;
			int y=p.y+cy;
			int xp=x-(dim/2);
	        int yp=y-(dim/2);
	        Rectangle rect=new Rectangle(xp,yp,dim,dim);
	        //check used slots
	        int nSlots=slots.size();
	        boolean allocated=false;
	        for(int j=0;j<nSlots;j++){
	        	//check each existing slots
	        	Rectangle r=(Rectangle)slots.get(j);
	        	if(r.intersects(rect)){
	        		allocated=true;
	        		break;
	        	}
	        }
	        if(!allocated){
	        	slots.add(rect);
	        }
	        	
	     }
	     return slots.size();  
	}
	public void draw(Graphics g){
	
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		double centreX=((double)_rect.width/2);
		double centreY=((double)_rect.height/2);
		//flush child positions		
		reset();
		//draw kids first
		int ng=_kids.size();
		int dim=GLYPH_SIZE;
		if(ng>0){
			
			_points=new ArrayList();
			//calcPoints(_rect.x+(int)centreX,_rect.y+(int)centreY,_rect.width/2);
			//calc imaginary circel
			calcPoints(0,0,_rect.width/2);
			
			//now draw the kids on the boundaries
			
			int cx=_rect.x+(int)centreX;
			int cy=_rect.y+(int)centreY;
			
			
			while(countAvailableSlots(dim)<ng){
				dim--;	
			}
							
			int nPoints=_points.size();
	        int count=0;
						
			for(int i=0;i<nPoints && count<ng;i++){
				Point p=(Point)_points.get(i);
				//adjust point
				p.x+=cx;
				p.y+=cy;
				int xp=p.x-(dim/2);
		        int yp=p.y-(dim/2);
		        Rectangle rect=new Rectangle(xp,yp,dim,dim);    
				if(!overlaps(rect)){
					Glyph kid=(Glyph)_kids.get(count);
					//g.setColor(Color.lightGray);
					//g.drawLine(p.x,p.y,cx,cy);
					g2.setPaint(Color.lightGray);
					g2.setStroke(dashed);
					g2.draw(new Line2D.Double(p.x,p.y,cx,cy));
					g2.setStroke(normal);
					kid._rect=rect;
					kid.draw(g);
					//count+=inc;
					count++;
				}				
			}
		}
		
		//g.setColor(Color.lightGray);
		//g.drawRect(_rect.x,_rect.y,_rect.width,_rect.height);
				
		//centre the glyph
		
		//Image image=_image;
	//	int iWidth=_image.getWidth(null);
	
		if(_rect.width>=dim){
			
			//double ix=centreX-(iWidth/2);
			//double iy=centreY-(image.getHeight(null)/2);
			//ix+=_rect.x;
			//iy+=_rect.y;
			//g.drawImage(image,(int)ix,(int)iy,null);
			Color color=_isRoot?LUS_COLOR:SERVICE_COLOR;
			Rectangle prefRect=getPreferredRect();
			int x=prefRect.x;
			int y=prefRect.y;
			int wid=prefRect.width;
			int hi=prefRect.height;
				
			GradientPaint bluetowhite = 
				new GradientPaint(x,y,color,x+wid, y+hi,Color.white);
        	g2.setPaint(bluetowhite);
	        g2.fill (new Ellipse2D.Double(x,y,wid,hi));
	        
			
		} 
		boolean fits=false;
		Font font=g2.getFont();
		font=new Font(font.getFamily(),Font.PLAIN,10);
		if(_userObject!=null && _userObject instanceof ServiceNode){
			_text=((ServiceNode)_userObject).getName();
		}
		String label=_text;
	
		while(!fits){
			FontMetrics fm=g2.getFontMetrics(font);
			int width=fm.stringWidth(label);
			if(width<(GLYPH_SIZE-5)){
				fits=true;
			}else{
				//font=new Font(font.getFamily(),Font.BOLD,font.getSize()-1);
				label=label.substring(0,label.length()-1);
			}
			
		}
		
		g2.setFont(font);
		FontMetrics fm=g2.getFontMetrics(font);
	
		//centreY-=(double)fm.getHeight()/2;
	
		int strWidth=fm.stringWidth(label);
		double xPos=centreX-((double)strWidth/2);
		g2.setPaint(Color.black);
		g2.drawString(label,(int)xPos+_rect.x,(int)centreY+_rect.y);
	}
	public void highlight(Graphics g){
	
		g.setColor(Color.yellow);
		Rectangle prefRect=getPreferredRect();
		int x=prefRect.x;
		int y=prefRect.y;
		int wid=prefRect.width;
		int hi=prefRect.height;

		g.drawRect(x,y,wid,hi);
		
	}	
	private void calcPoints(int cx, int cy, int r){
			
		int x = 0;
        int y = r;
        int d = 1 - r;
        while(y > x){
        	if(d < 0){
            	d += 2 * x + 3;
            }else{
            	d += 2 * (x - y) + 5;
                y --;
            }
            x ++;
			if(x%5==1){
							
            	if(y != r){
	            	_points.add( new Point(cx + x - 1, cy + y));
	                _points.add( new Point(cx - x + 1, cy + y));
	                
	                _points.add( new Point(cx - x + 1, cy - y));
	                _points.add( new Point(cx + x - 1, cy - y));
	             }
	             _points.add( new Point(cy + y - 1, cx + x));
	             _points.add( new Point(cy - y + 1, cx + x));
	                 
                        
				 _points.add( new Point(cy - y + 1, cx - x));
	             _points.add( new Point( cy + y - 1, cx - x));
	        }
			/*
                //now draw some rectangles around the circel
                int n=1;
                int nPoints=points.size();
                int inc=nPoints/n;
                
                int w=10;
                int h=10;
                
                for(int i=0;i<nPoints;i+=inc){
                	Point p=(Point)points.get(i);
                	int xp=p.x-(w/2);
                	int yp=p.y-(h/2);
                	g.drawRect(xp,yp,w,h);
                	
                }
             */   
        }
	}
}
