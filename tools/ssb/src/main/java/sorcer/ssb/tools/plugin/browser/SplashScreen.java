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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

public class SplashScreen extends Window {
	private Image _image;

	public SplashScreen(Frame parent, Image image) {
		super(parent);
		_image = image;
		// setAlwaysOnTop(true);
		setSize(image.getWidth(null), image.getHeight(null));

		/* Center the window */
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle winDim = getBounds();
		setLocation((screenDim.width - winDim.width) / 2,
				(screenDim.height - winDim.height) / 2);
		setVisible(true);
	}

	public void paint(Graphics g) {
		if (_image != null) {
			g.drawImage(_image, 0, 0, this);
		}
	}
}
