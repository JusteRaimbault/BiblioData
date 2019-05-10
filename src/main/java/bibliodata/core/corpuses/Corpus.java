/**
 * 
 */
package bibliodata.core.corpuses;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import bibliodata.scholar.ScholarAPI;
import bibliodata.utils.*;
import bibliodata.core.reference.Reference;

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
		GEXFWriter.writeCitationNetwork(file,new DefaultCorpus(references));
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
		Log.stdout("Exporting corpus with "+datanodes.size()+" refs, "+dataedges.size()+" links");
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
			for(String attr:attributes){row.add(r.getAttribute(attr));}
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



	// TODO take into account different depths here ? (rq several files)

	/**
	 * Import from file an "initial corpus", i.e. with no links besides an optional cited folder
	 * @param refFile
	 * @param citedFolder
	 * @return
	 */
	public static Corpus fromNodeFile(String refFile, String citedFolder){
		Corpus initial = new DefaultCorpus();

		if(refFile.endsWith(".ris")){
			RISReader.read(refFile,-1);
			// possible unexpected behavior as corpus set was the map keyset ? no as default corpus copies in a new HashSet - but mistake in principle
			//initial = new DefaultCorpus(Reference.references.keySet());
			initial = new DefaultCorpus(Reference.getReferences());
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
			// idem
			initial = new DefaultCorpus(Reference.getReferences());
		}

		return(initial);
	}

	public static OrderedCorpus fromCSV(String file){
		return(new CSVFactory(file).getCorpus());
	}

	public static Corpus fromCSV(String file,
								 String orderFile,
								 String citationFile,
								 String citedFolder,
								 int initDepth,
								 String origin){
		Corpus initial = Corpus.fromNodeFile(file,citedFolder);
		Log.stdout("Imported corpus of size "+initial.references.size());

		//update the origin
		for(Reference r:initial){
			//if(orderFile.length()!=0){r.depth=initDepth;} // done later
			r.origin=origin;
		}

		// add citations
		if(citationFile.length()>0) {
			String[][] rawlinks = CSVReader.read(citationFile, ";", "\"");
			for (String[] link : rawlinks) {
				String from = link[0];
				String to = link[1];
				// ! do not access Reference.references directly -> use construct instead (done for that)
				//Reference.references.get(to).citing.add(Reference.references.get(from));
				Reference.construct(to).citing.add(Reference.construct(from));
			}
		}


		// add order if needed
		if(orderFile.length()!=0){
			OrderedCorpus order = Corpus.fromCSV(orderFile);
			for(int i = 0 ; i<order.orderedRefs.size();i++){
				//order.orderedRefs.get(i).horizontalDepth.put(origin,new Integer(i));
				order.orderedRefs.get(i).setHorizontalDepth(origin,i);
				order.orderedRefs.get(i).setDepth(initDepth);
			}
		}

		return(initial);
	}





	
}
