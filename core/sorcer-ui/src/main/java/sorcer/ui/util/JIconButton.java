package sorcer.ui.util;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * A regular JButton created with an ImageIcon and with borders and content
 * areas turned off.
 */

public class JIconButton extends JButton {
	public JIconButton(String file) {
		super(new ImageIcon(file));
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
	}
	
	public JIconButton(URL url) {
		super(new ImageIcon(url));
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
	}
}
