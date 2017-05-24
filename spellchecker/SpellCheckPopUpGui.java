
package spellchecker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;

/**
 * @author epetruk
 * @date May 23, 2017
 * 
 *       This class is used to great a gui when the spell check have completed.
 *       It will display the spelling errors and the best corrections known
 *       relative to the loaded dictionary. It will display option to ignore
 *       spelling error, ignore all spelling errors of the same word, change
 *       spelling error based on list selection or change all spelling errors of
 *       words based list selection
 */
public class SpellCheckPopUpGui extends JFrame implements ActionListener {

	private static final long serialVersionUID = -4500018974478532422L;

	// gui components
	JLabel spellingError;
	JButton ignore, ignoreAll, change, changeAll, save;
	JList<String> list;
	DefaultListModel<String> model;
	JTextField textField;

	// if change was selected from last save point
	Boolean madeChanges;

	// the executed regular expression on the file to spell check. Used for
	// string replacments
	Matcher matcher;

	// bunch up the words and all their start indexs in their local file. Useful
	// when user selects change all.
	LinkedHashMap<String, LinkedList<Integer>> wordMatchersOnFile;
	LinkedList<Integer> currentWordMatchers;

	// list of skippalbe words, i.e. added after user selects change all or
	// ignore all
	HashSet<String> skippableWords;

	// string used to save changes to file
	StringBuffer fileOutput;

	// stores the changes made by the user
	HashMap<Integer, String> replacementStrings;

	// all words found in file. Used for iterating to display words in order
	LinkedList<String> allWords;
	Iterator<String> iterateSpellingErrors;
	String currentSpellingError;

	// all spelling errors and their proposed corrections
	LinkedHashMap<String, Set<String>> wordSpellingCorrections;

	// file to spell check with it's information
	FileToSpellCheck fileToSpellCheck;

	SpellCheckPopUpGui() {

		setLayout(new GridBagLayout());
		setTitle("English Spell Checker");
		Insets insets = new Insets(2, 2, 2, 2);
		GridBagConstraints gridConstraints = new GridBagConstraints();
		getContentPane().setBackground(Color.black);

		// the label to display the text of the spelling error
		spellingError = new JLabel();
		spellingError.setPreferredSize(new Dimension(400, 50));
		Font labelFont = spellingError.getFont();
		spellingError.setFont(new Font(labelFont.getName(), Font.PLAIN, 40));
		spellingError.setForeground(Color.red);
		gridConstraints.insets = new Insets(0, 0, 0, 0);
		gridConstraints.gridx = 0;
		gridConstraints.gridy = 0;
		gridConstraints.gridwidth = 2;
		gridConstraints.weighty = 0.0;
		gridConstraints.weightx = 1.0;
		gridConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridConstraints.insets = insets;
		add(spellingError, gridConstraints);

		// ignore spelling error buttons
		ignoreAll = new JButton("Ignore All");
		ignoreAll.addActionListener(this);
		gridConstraints.gridx = 1;
		gridConstraints.gridy = 1;
		gridConstraints.weighty = 0.0;
		gridConstraints.gridwidth = 1;
		add(ignoreAll, gridConstraints);
		ignore = new JButton("Ignore");
		ignore.addActionListener(this);
		gridConstraints.gridx = 0;
		gridConstraints.gridy = 1;
		gridConstraints.gridwidth = 1;
		gridConstraints.weighty = 0.0;
		add(ignore, gridConstraints);

		// list of spelling corrections
		model = new DefaultListModel<String>();
		list = new JList<String>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFont(new Font(labelFont.getName(), Font.PLAIN, 20));
		list.setBorder(new LineBorder(Color.BLACK));
		list.setCellRenderer(getRenderer());
		JScrollPane scrollPane = new JScrollPane(list);
		gridConstraints.weighty = 1.0;
		gridConstraints.gridwidth = 2;
		gridConstraints.gridx = 0;
		gridConstraints.gridy = 2;
		gridConstraints.fill = GridBagConstraints.BOTH;
		add(scrollPane, gridConstraints);

		// change spelling error buttons
		changeAll = new JButton("Change All");
		changeAll.addActionListener(this);
		gridConstraints.gridx = 1;
		gridConstraints.gridy = 3;
		gridConstraints.weighty = 0.0;
		gridConstraints.gridwidth = 0;
		add(changeAll, gridConstraints);
		change = new JButton("Change");
		change.addActionListener(this);
		gridConstraints.gridx = 0;
		gridConstraints.gridy = 3;
		gridConstraints.gridwidth = 1;
		gridConstraints.weighty = 0.0;
		add(change, gridConstraints);

		// save changes button
		save = new JButton("Save");
		save.addActionListener(this);
		gridConstraints.gridx = 0;
		gridConstraints.gridy = 4;
		gridConstraints.gridwidth = 2;
		gridConstraints.weighty = 0.0;
		gridConstraints.weightx = 1.0;
		gridConstraints.fill = GridBagConstraints.HORIZONTAL;
		// load save image
		try {
			Image img = ImageIO.read(getClass().getResource("images/save.png"));
			Image newImg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
			save.setIcon(new ImageIcon(newImg));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		add(save, gridConstraints);

	}

	/**
	 * Updates the GUI to display the spelling error along with proposals for
	 * correcting.
	 */
	public void nextSpellingError() {

		try {
			currentSpellingError = iterateSpellingErrors.next();
			// is spelling error already has ignore all or change all submission
			// or is not an error iterate to next word.
			while (skippableWords.contains(currentSpellingError)
					|| !wordSpellingCorrections.containsKey(currentSpellingError)) {
				currentSpellingError = iterateSpellingErrors.next();
			}

			// when iterator is complete close the window
		} catch (NoSuchElementException e) {
			dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
			return;
		}

		// display the error
		spellingError.setText(currentSpellingError);

		// clear the list of correction proposals
		model.clear();

		// add new correction proposals
		Set<String> wordCorrectionProposals = new HashSet<String>();
		Boolean uppercase = false, firstUpper = false;
		if (currentSpellingError.equals(currentSpellingError.toUpperCase())) {
			uppercase = true;
		} else if (currentSpellingError.charAt(0) == Character.toUpperCase(currentSpellingError.charAt(0))) {
			firstUpper = true;
		}
		for (String dictWord : wordSpellingCorrections.get(currentSpellingError)) {
			if (uppercase) {
				dictWord = dictWord.toUpperCase();
			} else if (firstUpper) {
				dictWord = dictWord.substring(0, 1).toUpperCase() + dictWord.substring(1);
			}
			wordCorrectionProposals.add(dictWord);
		}
		for (String tempWord : wordCorrectionProposals) {
			model.addElement(tempWord);
		}

		// update the list
		list.setModel(model);
		list.setSelectedIndex(0);

	}

	/**
	 * Display line borders on the list displaying word correction offers.
	 * 
	 * @author //https://stackoverflow.com/questions/23495012/jlist-how-to-get-a-lineborder-between-cells
	 * @return list renderer that is called for displaying the cell if added to
	 *         JList
	 */
	public ListCellRenderer<? super String> getRenderer() {
		return new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);
				listCellRendererComponent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
				return listCellRendererComponent;
			}
		};
	}

	/**
	 * @param spellingCorrections
	 * @param file
	 * @param gui
	 */
	public void showGui(FileToSpellCheck file, SpellCheckMainGui gui) {
		// new string to save file output
		fileOutput = new StringBuffer();

		// keep track of any words user wants to skip
		skippableWords = new HashSet<String>();

		// file to spell check and it's words
		fileToSpellCheck = file;
		allWords = fileToSpellCheck.allWords;

		// any changes made by user flag
		madeChanges = false;

		// save gui for changing visibility, spelling errors and log.
		SpellCheckMainGui mainGui = gui;
		wordSpellingCorrections = new LinkedHashMap<String, Set<String>>(gui.wordSpellingCorrections);

		// when window is closed save if changes made and reopen main gui at
		// position of current gui.
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				checkToSaveChanges();
				Dimension d = getContentPane().getSize();
				Insets i = getInsets();
				d.height += i.bottom + i.top;
				d.width += i.left + i.right;
				mainGui.setSize(d);
				mainGui.setLocation(getLocation());
				setVisible(false);
				mainGui.startButton.setEnabled(true);
				mainGui.log.append("Completed spell check.\n", Color.GREEN);
				mainGui.setVisible(true);
			}
		});

		// set to nothing so window listner is called
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		// store new copy because values are changed in this map.
		wordMatchersOnFile = new LinkedHashMap<String, LinkedList<Integer>>(fileToSpellCheck.wordsToCheck);

		// store and strings the user wants to replace until they accept a save.
		replacementStrings = new HashMap<Integer, String>();

		// reset the matcher already performed for locating strings
		matcher = fileToSpellCheck.matcher.reset();

		// iterate over all words
		iterateSpellingErrors = allWords.iterator();

		// display first error
		nextSpellingError();

		// open at position and size of main gui
		setLocation(mainGui.getLocation());
		// default size so user know's window is visible
		setResizable(true);
		pack();
		setMinimumSize(new Dimension(460, 400));
		Dimension d = mainGui.getContentPane().getSize();
		Insets i = mainGui.getInsets();
		d.height += i.bottom + i.top;
		d.width += i.left + i.right;
		setSize(d);
		mainGui.setVisible(false);
		setVisible(true);
	}

	/**
	 * if made changes ask if user wants to save them
	 */
	public void checkToSaveChanges() {
		if (madeChanges) {

			int reply = JOptionPane.showConfirmDialog(this, "Would you like to save the changes?", "Save Changes",
					JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.YES_OPTION) {
				saveChangesToFile();
			}
		}

	}

	/**
	 * Save changes to file
	 */
	public void saveChangesToFile() {

		// for valid word see if replacement string save it's start index.
		while (matcher.find()) {
			if (replacementStrings.containsKey(matcher.start())) {
				// append the string replacement on the file
				matcher.appendReplacement(fileOutput, replacementStrings.get(matcher.start()));
			}
		}
		// append the rest of the string to the replacements and send to the
		// file
		matcher.appendTail(fileOutput);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSpellCheck.file))) {
			writer.write(fileOutput.toString());
		} catch (IOException except) {
			except.printStackTrace();
		}

		// update words after saving in case the user closes and reruns
		fileToSpellCheck.storeWordsForSpellChecking();
		// change all could force matcher to go to end so reset
		matcher.reset();
		// don't grow String buffer on consecutive saves.
		fileOutput = new StringBuffer();
		// another save shouldn't work unless changes are made
		madeChanges = false;

	}

	/**
	 * @param word
	 *            to replace
	 * @param replacement
	 *            string
	 * @param isChangeAll
	 *            need to know when to change all instances of word or not
	 */
	public void storeReplacement(String word, String replacement, Boolean isChangeAll) {
		currentWordMatchers = wordMatchersOnFile.get(word);
		madeChanges = true;
		for (int index = 0; index < currentWordMatchers.size(); index++) {
			// all matches of word in file store the start index of the string
			// and it's replacement when change all is selected.
			if (isChangeAll) {
				replacementStrings.put(Math.abs(currentWordMatchers.get(index)), replacement);
				continue;
			}
			// if index is ignored this isn't the instance of the word found in
			// the file
			else if (currentWordMatchers.get(index) < 0) {
				continue;
			}
			// when change button selected store the replacement string then
			// signal that the index has been looked at by negation
			else {
				replacementStrings.put(currentWordMatchers.get(index), replacement);
				currentWordMatchers.set(index, -currentWordMatchers.get(index));
				break;
			}
		}
	}

	/**
	 * @param word:
	 *            ignore spelling error
	 */
	public void storeIgnoreInstance(String word) {
		currentWordMatchers = wordMatchersOnFile.get(word);
		for (int index = 0; index < currentWordMatchers.size(); index++) {
			// signal that the index has been looked at by negation
			if (currentWordMatchers.get(index) > 0) {
				currentWordMatchers.set(index, -currentWordMatchers.get(index));
				break;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

		// store ignore instance string index, then display next error
		if (event.getSource() == ignore) {
			storeIgnoreInstance(currentSpellingError);
			nextSpellingError();
		}

		// store that user wants to ignore all errors of this word, then display
		// next word
		else if (event.getSource() == ignoreAll) {
			addWordToSkippable(currentSpellingError);
			nextSpellingError();
		}

		// save the string replacement, then display next word
		else if (event.getSource() == change) {
			storeReplacement(currentSpellingError, getSelectedListCell(), false);
			nextSpellingError();
		}

		// save all string replacements for this word, then display next word
		else if (event.getSource() == changeAll) {
			storeReplacement(currentSpellingError, getSelectedListCell(), true);
			addWordToSkippable(currentSpellingError);
			nextSpellingError();
		}

		// save changes by running new thread to update button
		else if (event.getSource() == save) {
			save.setEnabled(false);
			if (madeChanges) {
				new Thread(new Runnable() {
					public void run() {
						saveChangesToFile();
						save.setEnabled(true);
					}
				}).start();
			}
		}
	}

	/**
	 * @return value for word correction
	 */
	public String getSelectedListCell() {
		return list.getSelectedValue();
	}

	/**
	 * @param word
	 *            to skip if seen again
	 */
	public void addWordToSkippable(String word) {
		skippableWords.add(word);
	}

}
