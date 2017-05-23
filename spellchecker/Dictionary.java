package spellchecker;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author epetruk
 *
 * Represents the dictionary file the user loads. Holds unique words found in dictionary.
 */
public class Dictionary extends SpellCheckFile {
	
	Set<String> uniqueWords;
	
	Dictionary(File paramFile){
		super(paramFile);
	}

	/**
	 * @return
	 * 
	 *  store all unique words found in dictionary
	 */
	public int storeUniqueWords(){
		try{
			uniqueWords= new HashSet<String>();
			// not the best pattern but sufficient if one is only looking for word spelling errors
			pattern=Pattern.compile("\\b[\\w']+\\b");
			matcher= pattern.matcher(readFile(file));
			while (matcher.find() ){
				// don't want duplicates, so save runtime by not being case sensitive
				potentialWord=matcher.group().toLowerCase();
				// if word is not just numbers
				if (potentialWord.matches(".*[a-zA-Z]+.*")){				
					uniqueWords.add(potentialWord);
				}
			}
			return 0;
		}
		catch(IOException exception){
			return INVALID;
		}
		
	}
	
}
