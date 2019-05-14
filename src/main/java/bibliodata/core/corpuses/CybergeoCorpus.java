/**
 * 
 */
package bibliodata.core.corpuses;

import java.util.HashSet;

import bibliodata.scholar.ScholarAPI;
import bibliodata.utils.Log;
import bibliodata.core.reference.Reference;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class CybergeoCorpus extends Corpus {
	
	/**
	 * 
	 * 
	 * @param refs
	 */
	public CybergeoCorpus(HashSet<Reference> refs){
		references = refs;
		
		// add attribute
		for(Reference r:references){
			r.attributes.put("cybergeo", "1");
		}
	}

	
	public CybergeoCorpus(Reference r){
		//r.attributes.put("cybergeo", "1");
		references = new HashSet<Reference>();
		references.add(r);
	}
	
	
	
	/**
	 * Construct cited refs of cybergeo corpus
	 */
	public void fillCitedRefs(){
		int totalRefs = references.size();int p=0;
		for(Reference r:references){
			HashSet<Reference> verifiedCited=new HashSet<Reference>();
			for(Reference ghost:r.biblio.cited){
				Log.stdout("     Cited : "+ghost.getTitle().title);
				Reference cr = ScholarAPI.getScholarRef(ghost.getTitle().title,"",ghost.getYear());
				if(cr!=null){
					verifiedCited.add(cr);
					cr.setCiting(r);
				}
			}
			
			// dirty dirty
			r.biblio.cited = verifiedCited;
			
			/**
			 * TODO : write a generic constructor from ref title, combining mendeley and scholar requests to have most info possible ?
			 */
			
			//recompute citedTitles : may slightly differ after scholar request
			r.biblio.citedTitles.clear();
			for(Reference cr:r.biblio.cited){r.biblio.citedTitles.add(cr.getTitle().title);}
			
			Log.purpose("progress","Corpus "+name+" : cited refs : "+(100.0 * (1.0*p) / (1.0*totalRefs))+ " % ; ref : "+r.toString());p++;
		}
	}
	
	
}

