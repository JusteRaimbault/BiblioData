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

import static bibliodata.utils.ConversionUtils.toArray;

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
		LinkedList<String[]> datanodes = refsAsCSVRows(withAbstract,new LinkedList<>());
		LinkedList<String[]> dataedges = linksAsCSVRows();
		CSVWriter.write(prefix+".csv", datanodes, ";", "\"");
		CSVWriter.write(prefix+"_links.csv", dataedges, ";", "\"");
	}

	/**
	 * Export to csv with attributes
	 * @param prefix
	 * @param withAbstract
	 * @param attributes
	 */
	public void csvExport(String prefix,boolean withAbstract,LinkedList<String> attributes){
		LinkedList<String[]> datanodes = refsAsCSVRows(withAbstract,attributes);
		LinkedList<String[]> dataedges = linksAsCSVRows();
		CSVWriter.write(prefix+".csv", datanodes, ";", "\"");
		CSVWriter.write(prefix+"_links.csv", dataedges, ";", "\"");
	}

	private LinkedList<String[]> refsAsCSVRows(boolean withAbstract,LinkedList<String> attributes){
		LinkedList<String[]> res = new LinkedList<>();

		LinkedList<String> provheader = new LinkedList<String>();provheader.add("id");provheader.add("title");provheader.add("year");
		if(withAbstract){provheader.add("abstract");provheader.add("authors");}
		for(String attr:attributes){provheader.add(attr);}
		res.add(toArray(provheader));

		for(Reference r:references){
			LinkedList<String> row = new LinkedList<>();
			row.add(r.scholarID);row.add(r.title.title);row.add(r.year);
			if(withAbstract){row.add(r.resume.resume);row.add(r.getAuthorString());}
			for(String attr:attributes){provheader.add(r.getAttribute(attr));}
			res.add(toArray(row));
		}
		return(res);
	}

	private LinkedList<String[]> linksAsCSVRows(){
		LinkedList<String[]> dataedges = new LinkedList<String[]>();
		for(Reference r:references){for(Reference rc:r.citing){String[] edge = {rc.scholarID,r.scholarID};dataedges.add(edge);}}
		return(dataedges);
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


	/*
	public static Corpus fromMongo() {
		-> implemented in mongoimport
	}
	*/


	
}
