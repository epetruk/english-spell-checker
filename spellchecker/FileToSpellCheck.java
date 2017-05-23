package spellchecker;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * @author epetruk
 *
 *Represents the file the user loads. Holds all words found in file and separately holds start indexes of each word found.
 */
public class FileToSpellCheck extends SpellCheckFile {
	
	// keep track of words by their indexes
	LinkedHashMap<String,LinkedList<Integer>> wordsToCheck;
	// all found words
	LinkedList<String> allWords;
	
	public FileToSpellCheck(File paramFile) {
		super(paramFile);
	}
	/**
	 * @return 
	 * 
	 * save the index of words. ALso save all words found in order; order is important for displaying words as they appear in the file to the user. 
	 * 
	 */
	public int storeWordsForSpellChecking(){
		try{
			
			wordsToCheck=new LinkedHashMap<String, LinkedList<Integer>>();
			allWords=new LinkedList<String>();
			// not the best pattern but sufficient if one is only looking for word spelling errors
			pattern=Pattern.compile("\\b[\\w']+\\b");
			matcher= pattern.matcher(readFile(file));
			while (matcher.find() ){
				potentialWord=matcher.group();
				// is word is not just numbers
				if (potentialWord.matches(".*[a-zA-Z]+.*")){				
					if(!wordsToCheck.containsKey(potentialWord)){
						wordsToCheck.put(potentialWord, new LinkedList<Integer>());
					}
					wordsToCheck.get(potentialWord).add(matcher.start());
					allWords.add(potentialWord);
					
				}
			}
			return 0;
		}
		catch(IOException exception){
			return INVALID;
		}
	}
	
}
