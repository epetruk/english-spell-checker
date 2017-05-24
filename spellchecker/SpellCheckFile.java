package spellchecker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author epetruk
 * 
 *         abstract class outlining some implementation and structure for
 *         subclasses
 */
abstract public class SpellCheckFile {

	// pattern to match words in file
	Pattern pattern;

	File file;

	// find matches from pattern
	Matcher matcher;

	// match found
	String potentialWord;

	// when error return standard value
	final int INVALID = 1;

	SpellCheckFile(File paramFile) {
		file = paramFile;
	}

	/**
	 * @param file
	 *            read this file to string
	 * @return
	 * @throws IOException
	 */
	static String readFile(File file) throws IOException {

		byte[] encoded = Files.readAllBytes(file.toPath());
		return new String(encoded, Charset.defaultCharset());

	}

}
