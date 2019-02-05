/**
 * 
 */
package bibliodata.core.corpuses;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import bibliodata.scholar.ScholarAPI;
import bibliodata.utils.BIBReader;
import bibliodata.utils.CSVWriter;
import bibliodata.utils.GEXFWriter;
import bibliodata.core.reference.Reference;
import bibliodata.utils.RISReader;

/**
 * A corpus is a set of references
 * 
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public abstract class Corpus implements Iterable<Reference> {
	
	/**
	 * References in the corpus
	 */
	public HashSet<Reference> references;

	/**
	 * Name of the corpus
	 */
	public String name;
	
	
	public Corpus fillScholarIDs(){
		ScholarAPI.fillIds(references);
		return this;
	}
	
	
	/**
	 * Get citing refs.
	 * 
	 * @return this corpus
	 */
	public Corpus fillCitingRefs(){
		ScholarAPI.fillIdAndCitingRefs(this);
		return this;
	}
	
	
	/**
	 * Get the corpus of refs citing - assumes citing refs have been filled,
	 * only construct a wrapper around a new hashSet.
	 * 
	 * @return
	 */
	public Corpus getCitingCorpus(){
		HashSet<Reference> citing = new HashSet<Reference>();
		for(Reference r:references){
			for(Reference c:r.citing){
				citing.add(c);
			}
		}
		return new DefaultCorpus(citing);
	}
	
	public Corpus getCitedCorpus(){
		HashSet<Reference> cited = new HashSet<Reference>();
		for(Reference r:references){
			for(Reference rc:r.biblio.cited){
				cited.add(rc);
			}
		}
		return new DefaultCorpus(cited);
	}
	
	/**
	 * Get abstracts using Mendeley api.
	 * 
	 * @return
	 */
	public Corpus getAbstracts(){
		
		return this;
	}
	
	
	/**
	 * Write this corpus to gexf file
	 */
	public void gexfExport(String file){
		GEXFWriter.writeCitationNetwork(file,references);
	}
	
	
	/**
	 * Export to csv
	 * 
	 * @param prefix
	 * @param withAbstract
	 */
	public void csvExport(String prefix,boolean withAbstract){
		LinkedList<String[]> datanodes = new LinkedList<String[]>();
		LinkedList<String[]> dataedges = new LinkedList<String[]>();
		if(!withAbstract){String[] header = {"title","id","year"};datanodes.add(header);}else{String[] header = {"title","id","year","abstract","year"};datanodes.add(header);}
		for(Reference r:references){
			String[] row = {""};
			if(!withAbstract){String[] tmp = {r.title.title,r.scholarID,r.year};row=tmp;}
			else{
				String authorstr = "";for(String s:r.authors){authorstr=authorstr+s+",";}
				String[] tmp = {r.title.title,r.scholarID,r.year,r.resume.resume,authorstr};
				row=tmp;
			}
			datanodes.add(row);
			for(Reference rc:r.citing){String[] edge = {rc.scholarID,r.scholarID};dataedges.add(edge);}	
		}
		CSVWriter.write(prefix+".csv", datanodes, ";", "\"");
		CSVWriter.write(prefix+"_links.csv", dataedges, ";", "\"");
	}
	
	
	
	/**
	 * Iterable type.
	 */
	public Iterator<Reference> iterator(){
		return references.iterator();
	}


	@Override
	public String toString() {
		return("Corpus "+name+" ("+references.size()+" refs)");
	}




	public static Corpus fromFile(String refFile, String citedFolder){
		Corpus initial = new DefaultCorpus();

		if(refFile.endsWith(".ris")){
			RISReader.read(refFile,-1);
			initial = new DefaultCorpus(Reference.references.keySet());
		}
		if(refFile.endsWith(".csv")){
			if(citedFolder.length()==0){
				initial = new CSVFactory(refFile).getCorpus();
			}else{
				initial = new CSVFactory(refFile,-1,citedFolder).getCorpus();
			}
		}

		if(refFile.endsWith(".bib")){
			BIBReader.read(refFile);
			initial = new DefaultCorpus(Reference.references.keySet());
		}

		return(initial);
	}


	
}
