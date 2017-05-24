package spellchecker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author epetruk
 * @date   May 23, 2017
 * 
 * This project spell checks a .txt file uploaded by the user via a GUI. The spell check displays an additional page after the spell check completes if spelling errors were found; 
 * this additional page displays buttons to make changes to the errors found and to save them back to the original file. 
 * The spell check is done relative to a dictinary .txt file that is also uploaded by the user. 
 * Thus, a spelling error is a word instance found in the spell checked file that goes without a match on the dictionary file, i.e. spelling is all that is checked.
 */
/**
 * @author epetruk
 *
 */
public class SpellCheckMainGui extends JFrame implements ActionListener {

	private static final long serialVersionUID = -7957858494133020315L;

	// main Gui components
	JButton openDictionaryButton, openFileToSpellCheck, startButton;
	Log log = new Log();
	JScrollPane scrollPane;
	JFileChooser fileChooser;

	// files required to start spelling checks
	Dictionary dictionaryFile;
	FileToSpellCheck fileToSpellCheck;

	// holds spelling errors with proposed strings in a set.
	// Insertion order is maintained with LinkedHashMap which is important for
	// preserving order from best match to worst.
	LinkedHashMap<String, Set<String>> wordSpellingCorrections = new LinkedHashMap<String, Set<String>>();

	// how many word correction proposals are saved and displayed
	final Integer MAXNUMOFWORDPROPOSALS = 30;

	// multi-threaded the loading of the files. Useful when files are large to
	// load both at same time
	// increment when loading and decrement when done.
	Integer fileLoadsInProgress = 0;

	SpellCheckMainGui() {

		setLayout(new BorderLayout());
		setTitle("English Spell Check");

		// check if user is running java 8
		String version = System.getProperty("java.version");
		int pos = version.indexOf('.');
		pos = version.indexOf('.', pos + 1);
		Double dVersion = Double.parseDouble(version.substring(0, pos));
		if (dVersion < 1.8) {
			JOptionPane.showMessageDialog(null, "Please install Java version 1.8 or higher to run this program.",
					"Unsupported Java Version", JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);
		}

		log.append("Welcome to English Spell Check.\n", Color.BLACK);
		JScrollPane scrollPane = new JScrollPane(log);

		// file chooser for loading files via the GUI
		fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
		fileChooser.setFileFilter(filter);

		openDictionaryButton = new JButton("Open Dictionary .txt File...");
		openDictionaryButton.addActionListener(this);
		openFileToSpellCheck = new JButton("Open .txt File To Spell Check...");
		openFileToSpellCheck.addActionListener(this);
		// add images to open buttons
		try {
			Image img = ImageIO.read(getClass().getResource("images/file.png"));
			Image newImg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
			openDictionaryButton.setIcon(new ImageIcon(newImg));
			openFileToSpellCheck.setIcon(new ImageIcon(newImg));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		startButton = new JButton("Start Spell Check");
		startButton.addActionListener(this);

		// panel to hold both open buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(Color.black);
		buttonPanel.add(openDictionaryButton);
		buttonPanel.add(openFileToSpellCheck);

		// add all the components
		add(buttonPanel, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
		add(startButton, BorderLayout.PAGE_END);

	}

	public static void main(String[] args) {
		SpellCheckMainGui gui = new SpellCheckMainGui();
		gui.showGui();

	}

	/**
	 * Primarily used to make this GUI visible and set default configs.
	 */
	public void showGui() {

		setResizable(true);
		pack();

		// default size so user know's window is visible
		setSize(600, 600);

		// preferably same size as pop-gui. 460 width holds both open buttons,
		// something slightly smaller doesn't
		setMinimumSize(new Dimension(460, 400));

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

	/**
	 * response to button clicks by user
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		// load dictionary from file chooser and validate dictionary is
		// something expected.
		if (event.getSource() == openDictionaryButton) {
			int returnValue = fileChooser.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				Dictionary tempDictionaryFile = new Dictionary(fileChooser.getSelectedFile());
				validateDictionary(tempDictionaryFile);
			} else {
				log.append("Cancelled open dictionary search.\n", Color.black);
			}

			// load file to spell check and do input validation.
		} else if (event.getSource() == openFileToSpellCheck) {
			int returnValue = fileChooser.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				FileToSpellCheck tempFileSpellCheck = new FileToSpellCheck(fileChooser.getSelectedFile());
				validateFileToSpellCheck(tempFileSpellCheck);
			} else {
				log.append("Cancelled open dictionary search.\n", Color.black);
			}

			// if start button clicked get spelling errors
		} else if (event.getSource() == startButton) {

			// check to make sure this window has a dictionary and file to spell
			// check loaded.
			if (fileToSpellCheck == null) {
				JOptionPane.showMessageDialog(startButton, "Please submit a file to spell check.", "No File Error",
						JOptionPane.INFORMATION_MESSAGE);
			} else if (dictionaryFile == null) {
				JOptionPane.showMessageDialog(startButton,
						"Please submit a dictionary file before starting the spell check.", "No File Error",
						JOptionPane.INFORMATION_MESSAGE);
			}

			// disable start button and grab the spelling errors with another
			// background thread, i.e. Supervisor.
			// must invoke additional thread to display log message and set
			// start button disabled
			else {
				startButton.setEnabled(false);
				wordSpellingCorrections = new LinkedHashMap<String, Set<String>>();
				log.append("Finding spelling errors, please wait...\n", Color.black);
				new Supervisor(this).execute();
			}
		}
	}

	/**
	 * supervisor worker that updates start button when completed and
	 * instantiates SpellCheckPopUpGui to allow user to modify file that was
	 * spell checked.
	 *
	 */
	private class Supervisor extends SwingWorker<Void, Void> {

		// for passing mainGui to pop-gui in done method.
		SpellCheckMainGui mainGui;

		Supervisor(SpellCheckMainGui gui) {
			mainGui = gui;
		}

		// evaluating large files could take a while, "doInBackground"
		@Override
		protected Void doInBackground() throws Exception {
//			long startTime = System.currentTimeMillis();
			try {
				findSpellingErrors();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			
//			long endTime = System.currentTimeMillis();
//			System.out.println("Total execution time: " + (endTime - startTime) );
			return null;
		}

		// after searched for spelling errors allow user to see these errors
		@Override
		protected void done() {
			startButton.setEnabled(true);
			SpellCheckPopUpGui gui = new SpellCheckPopUpGui();
			// if no spelling errors don't open the pop-up gui
			if (wordSpellingCorrections.keySet().size() == 0) {
				JOptionPane.showMessageDialog(mainGui, "Spell check complete, no spelling errors found.", "Finished",
						JOptionPane.INFORMATION_MESSAGE);
				mainGui.log.append("Completed spell check.\n", Color.GREEN);
			} else {
				gui.showGui(fileToSpellCheck, mainGui);
			}
		}
	}

	/**
	 * For each word in file see if there's a matching spelling in dictionary.
	 * If not error.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void findSpellingErrors() throws InterruptedException, ExecutionException {

		// each word in file is matched against all unique words in dictionary.
		// Each of these tasks returns a result stored in futures
		HashSet<Future<HashMap<String, HashMap<String, Integer>>>> futures = new HashSet<Future<HashMap<String, HashMap<String, Integer>>>>();

		// find available threads and start a thread pool
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		// Submit the file word to dictionary match tasks to thread pool and add
		// the returned objects to futures for retrieval.
		for (final String word : fileToSpellCheck.wordsToCheck.keySet()) {
			if (!dictionaryFile.uniqueWords.contains(word.toLowerCase())) {
				Future<HashMap<String, HashMap<String, Integer>>> future = (Future<HashMap<String, HashMap<String, Integer>>>) executor
						.submit(new Callable<HashMap<String, HashMap<String, Integer>>>() {
							public HashMap<String, HashMap<String, Integer>> call() {
								return checkSpelling(word);
							}
						});

				futures.add(future);
			}

		}
		// when task submitted tasks are finished free up resources consumed by
		// thread pool
		executor.shutdown();

		// for each file word that was matched against each dictionary word,
		// check for spelling error
		for (Future<HashMap<String, HashMap<String, Integer>>> future : futures) {
			HashMap<String, HashMap<String, Integer>> temp = future.get();
			for (Map.Entry<String, HashMap<String, Integer>> entry : temp.entrySet()) {
				try {
					wordSpellingCorrections.put(entry.getKey(), entry.getValue().keySet());
				} catch (NoSuchElementException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param word:
	 *            for each word in file compare to each word in dictionary via
	 *            levenshteinDistance algorithm to discover the best spelling
	 *            change proposals
	 * @return
	 */
	public HashMap<String, HashMap<String, Integer>> checkSpelling(String word) {

		HashMap<String, Integer> resultValue = new HashMap<String, Integer>();
		HashMap<String, HashMap<String, Integer>> result = new HashMap<String, HashMap<String, Integer>>();
		for (String dictWord : dictionaryFile.uniqueWords) {
			resultValue.put(dictWord, levenshteinDistance(dictWord.toLowerCase(), word.toLowerCase()));
		}
		// order the comparison by value which gives the best-to-worst word
		// matches found in dictionary.
		// of these best matches store a finite amount to save memory and then
		// dump this order in an insertion order
		// preserved collection.
		result.put(word, resultValue.entrySet().stream().sorted(Entry.comparingByValue()).limit(MAXNUMOFWORDPROPOSALS)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
		return result;
	}

	/**
	 * @return java version found on machine
	 */
	static double getVersion() {
		String version = System.getProperty("java.version");
		int pos = version.indexOf('.');
		pos = version.indexOf('.', pos + 1);
		return Double.parseDouble(version.substring(0, pos));
	}

	/**
	 * Evaluate how closely two string resemble each other. Return an integer to
	 * symbolize this integer to symbolize this resemblance
	 * 
	 * @author https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
	 * @param lhs
	 *            string #1
	 * @param rhs
	 *            string #2
	 * @return integer to symbolize resemblance of two input strings
	 */
	public static int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
		int len0 = lhs.length() + 1;
		int len1 = rhs.length() + 1;

		// the array of distances
		int[] cost = new int[len0];
		int[] newcost = new int[len0];

		// initial cost of skipping prefix in String s0
		for (int i = 0; i < len0; i++)
			cost[i] = i;

		// dynamically computing the array of distances

		// transformation cost for each letter in s1
		for (int j = 1; j < len1; j++) {
			// initial cost of skipping prefix in String s1
			newcost[0] = j;

			// transformation cost for each letter in s0
			for (int i = 1; i < len0; i++) {
				// matching current letters in both strings
				int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

				// computing cost for each transformation
				int cost_replace = cost[i - 1] + match;
				int cost_insert = cost[i] + 1;
				int cost_delete = newcost[i - 1] + 1;

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			int[] swap = cost;
			cost = newcost;
			newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return cost[len0 - 1];
	}

	/**
	 * Whether filename ends with "txt" extension.
	 * 
	 * @param fileName
	 *            to check if is txt or not
	 * @return
	 */
	public Boolean isTxtExtension(String fileName) {
		int i = fileName.lastIndexOf('.');
		String extension = "";
		if (i > 0) {
			extension = fileName.substring(i + 1).trim();
		}
		if (extension.equals("txt")) {
			return true;
		}
		return false;
	}

	/**
	 * @param dictionary:
	 *            validate the dictionary file loaded by the user.
	 */
	public void validateDictionary(Dictionary dictionary) {
		startButton.setEnabled(false);

		// mult-thread to load dictionary and other files at same time
		new Thread(new Runnable() {
			public void run() {
				openDictionaryButton.setEnabled(false);

				// keeps track of any ongoing loads by incrementing counter
				synchronized (fileLoadsInProgress) {
					fileLoadsInProgress++;
				}
				log.append("Loading Dictionary:" + dictionary.file + "\n", Color.BLACK);

				// grab all the unique words in the dictionary
				int returnValue = dictionary.storeUniqueWords();

				// check if dictionary was accessed and has at least one word
				if (returnValue == dictionary.INVALID) {
					log.append("Failed open dictionary search.\n", Color.red);
				} else if (dictionary.uniqueWords.size() < 1) {
					log.append(
							"Please add a dictionary that has words in it. This program is designed for .txt files.\n",
							Color.red);
				} else {

					// warn when not txt file
					if (!isTxtExtension(dictionary.file.getName())) {
						log.append(
								"You loaded a non txt file as a dictionary, beware that this program is intended for ASCII encoded files.\n",
								Color.yellow);
					}

					// assign to dictionaryFile here to ensure ditionaryFile is
					// only assigned a reference when successful. Later checks
					// on dictionaryFile ensure valid dictionary.
					dictionaryFile = dictionary;
					log.append("Successfully opened dictionary file with "
							+ Integer.toString(dictionary.uniqueWords.size()) + " words in it.\n", Color.green);
				}

				// re-enable button to load another dictionary
				openDictionaryButton.setEnabled(true);

				// keeps track of any ongoing file loads by modifying counter
				decrementLoads();
			}
		}).start();
	}

	/**
	 * @param tempFileSpellCheck
	 *            validate the file to spell check loaded by the user.
	 */
	public void validateFileToSpellCheck(FileToSpellCheck tempFileSpellCheck) {
		startButton.setEnabled(false);

		// mult-thread to load dictionary and other files at same time
		new Thread(new Runnable() {
			public void run() {
				openFileToSpellCheck.setEnabled(false);

				// keeps track of any ongoing file loads by modifying counter
				synchronized (fileLoadsInProgress) {
					fileLoadsInProgress++;
				}

				log.append("Loading file:" + tempFileSpellCheck.file + "\n", Color.BLACK);

				// grab all unique words in file
				int returnValue = tempFileSpellCheck.storeWordsForSpellChecking();

				// if file is accessed and has more than 50000 but at least one
				// word then accept it.
				// word limit was chosen to limit excessive runtimes. More than
				// 50000 is actually fine if someone is willing to wait 5 min or
				// more
				if (returnValue == tempFileSpellCheck.INVALID) {
					log.append("Failed to open file.\n", Color.red);

				} else if (tempFileSpellCheck.allWords.size() == 0) {
					log.append("Please add a file that has words in it. This program is designed for .txt files.\n",
							Color.red);
				}// else if (tempFileSpellCheck.allWords.size() > 50000) {
//					log.append("Please add a file that has less than 50000 words in it.\n", Color.red);
//				}

				else {

					// warn when not txt file
					if (!isTxtExtension(tempFileSpellCheck.file.getName())) {
						log.append(
								"You loaded a non txt file to spell check, beware that this program is intended for ASCII encoded files.\n",
								Color.yellow);
					}

					// assign to fileToSpellCheck here to ensure file is only
					// assigned a reference when successful.
					fileToSpellCheck = tempFileSpellCheck;
					log.append("Successfully opened file with " + Integer.toString(tempFileSpellCheck.allWords.size())
							+ " words to spell check.\n", Color.green);
				}
				openFileToSpellCheck.setEnabled(true);

				// keeps track of any ongoing file loads by modifying counter
				decrementLoads();
			}
		}).start();
	}

	/**
	 * keeps track of any ongoing file loads by modifying counter
	 */
	public void decrementLoads() {
		synchronized (fileLoadsInProgress) {
			if (--fileLoadsInProgress == 0) {
				startButton.setEnabled(true);
			}
		}
	}

}
