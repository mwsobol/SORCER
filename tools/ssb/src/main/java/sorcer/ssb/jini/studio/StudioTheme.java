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

import java.awt.Color;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

public class StudioTheme extends DefaultMetalTheme {
    
    public StudioTheme() {
        HIGHLIGHT_COLOR=primary3;
        QUOTE_COLOR=primary2;
    }
    
    public String getName() {
        return "SSB";
    }
    
    public FontUIResource getControlTextFont() {
        return controlFont;
    }
    
    public FontUIResource getSystemTextFont() {
        return systemFont;
    }
    
    public FontUIResource getUserTextFont() {
        return userFont;
    }
    
    public FontUIResource getMenuTextFont() {
        return controlFont;
    }
    
    public FontUIResource getWindowTitleFont() {
        return controlFont;
    }
    
    public FontUIResource getSubTextFont() {
        return smallFont;
    }
    
    protected ColorUIResource getPrimary2() {
        return primary2;
    }
    
    protected ColorUIResource getPrimary3() {
        return primary3;
    }
    
    private final FontUIResource controlFont = new FontUIResource("Dialog", 0, 11);
    private final FontUIResource systemFont = new FontUIResource("Dialog", 0, 11);
    private final FontUIResource userFont = new FontUIResource("SansSerif", 0, 11);
    private final FontUIResource smallFont = new FontUIResource("Dialog", 0, 10);
    private final ColorUIResource primary2 = new ColorUIResource(68, 126, 183);
    private final ColorUIResource primary3 = new ColorUIResource(153, 180, 255);
    
    static{
        new StudioTheme();
    }
    static Color HIGHLIGHT_COLOR;
    static Color QUOTE_COLOR;
    static Color MARGIN_COLOR=new Color(218,227,252);
}
