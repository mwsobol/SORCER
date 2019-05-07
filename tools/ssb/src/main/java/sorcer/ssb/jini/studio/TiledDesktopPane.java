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

package sorcer.ssb.jini.studio;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

public class TiledDesktopPane extends JDesktopPane {
    
        private JInternalFrame _jif[];
        private boolean initialized;
        
        public TiledDesktopPane(JInternalFrame [] jif){
            _jif=jif;
        }
        
        public void addNotify() {
            super.addNotify();
            
            if(initialized){
                return;
            }
            initialized=true;
            
            SwingUtilities.invokeLater(new Runnable() {
                
                public void run() {
                    for(Dimension d = getSize(); d.width == 0;) {
                        d = getSize();
                        try {
                            Thread.sleep(100L);
                        }
                        catch(Exception ex) {
                            return;
                        }
                    }
                    
                    tile();
                    addResizeAdapter();
                }
                
            });
        }
        
        private void addResizeAdapter() {
            addComponentListener(new ComponentAdapter() {
                
                public void componentResized(ComponentEvent evt) {
                    //System.out.println(evt);
                    if(evt.getID()==ComponentEvent.COMPONENT_RESIZED){
                        tile();
                    }
                }
                
            });
        }
        
        private void tile() {
            for(int i = 0; i < _jif.length; i++)
                _jif[i].toFront();
            
            Dimension dim = getSize();
            java.awt.Component comp[] = getComponents();
            int nwins = comp.length;
            if(nwins == 0)
                return;
            int offset = 0;
            double sqrt = Math.sqrt(nwins);
            int num = (int)sqrt;
            double xwid = dim.width / num;
            double fact = (double)nwins / (double)num;
            if(fact != (double)(int)fact)
                fact = (int)(fact + 1.0D);
            double yhi = (double)dim.height / fact;
            int xpos = 0;
            int ypos = 0;
            int count = 0;
            for(int i = 0; i < comp.length; i++) {
                JInternalFrame win = (JInternalFrame)comp[i];
                win.setBounds(xpos, ypos, (int)xwid, (int)yhi);
                count++;
                xpos = (int)((double)xpos + xwid);
                if(count == num) {
                    xpos = 0;
                    ypos = (int)((double)ypos + yhi);
                    count = 0;
                }
            }
            
        }
        
        TiledDesktopPane _tiledPane;
        
        
        
        TiledDesktopPane() {
            _tiledPane = this;
        }

}
