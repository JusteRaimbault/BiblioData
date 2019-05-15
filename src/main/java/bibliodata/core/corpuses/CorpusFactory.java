/**
 * 
 */
package bibliodata.core.corpuses;

import java.util.HashMap;
import java.util.HashSet;

import bibliodata.core.reference.Reference;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public interface CorpusFactory {
	
	/**
	 * Setup the factory with the given options (proper to each factory).
	 * 
	 * @param options setup options as an attribute map
	 */
	void setup(HashMap<String,String> options);
	
	/**
	 * Constructs the corpus and retrieves it.
	 * 
	 * @return the corpus
	 */
	Corpus getCorpus();
	
}
