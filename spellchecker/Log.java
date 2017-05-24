package spellchecker;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * @author epetruk
 * 
 *         Log object for displaying messages to user.
 *
 */
public class Log extends JTextPane {
	private static final long serialVersionUID = 640029538948203664L;
	StyledDocument logDocument;
	SimpleAttributeSet logAttributes;

	Log() {
		setEditable(false);
		logDocument = getStyledDocument();
		logAttributes = new SimpleAttributeSet();
		StyleConstants.setFontSize(logAttributes, 18);
		setBackground(Color.LIGHT_GRAY);
	}

	/**
	 * @param str
	 *            append this string to log
	 * @param color
	 *            text color
	 */
	public void append(String str, Color color) {
		try {
			StyleConstants.setForeground(logAttributes, color);
			logDocument.insertString(logDocument.getLength(), str, logAttributes);
			setCaretPosition(logDocument.getLength());
		} catch (BadLocationException exc) {
			exc.printStackTrace();
		}
	}

}
