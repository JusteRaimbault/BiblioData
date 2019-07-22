/**
 * 
 */
package bibliodata.core.corpuses;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import bibliodata.scholar.ScholarAPI;
import bibliodata.utils.*;
import bibliodata.core.reference.Reference;
import org.apache.commons.lang3.ArrayUtils;

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
	 * FIXME make this private as for the global hashmap ?
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
	 * Get citing refs (no consolidation)
	 * 
	 * @return this corpus
	 */
	public Corpus fillCitingRefs(){
		ScholarAPI.fillIdAndCitingRefs(this,"",false);
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
			citing.addAll(r.getCiting());
		}
		return new DefaultCorpus(citing);
	}
	
	public Corpus getCitedCorpus(){
		HashSet<Reference> cited = new HashSet<Reference>();
		for(Reference r:references){
			for(Reference rc:r.getBiblio().cited){
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
			row.add(r.getId());row.add(r.getTitle().title);row.add(r.getYear());
			if(withAbstract){row.add(r.getResume().resume);row.add(r.getAuthorString());}
			for(String attr:attributes){
				row.add(r.getAttribute(attr)); // NA if attribute is not available
			}
			res.add(toArray(row));
		}
		return(res);
	}

	private LinkedList<String[]> linksAsCSVRows(){
		LinkedList<String[]> dataedges = new LinkedList<String[]>();
		for(Reference r:references){for(Reference rc:r.getCiting()){String[] edge = {rc.getId(),r.getId()};dataedges.add(edge);}}
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

	/**
	 * Get an ordered corpus from file
	 * @param file
	 * @return
	 */
	public static OrderedCorpus fromCSV(String file){
		// FIXME with header or not is a mess
		//return(new CSVFactory(file,true).getCorpus());
		return(new CSVFactory(file,false).getCorpus());
	}

	/**
	 * Note : order file MUST be csv -> conversion for when initial corpus is bib
	 */
	public static Corpus fromCSV(String file,
								 String orderFile,
								 String citationFile,
								 String citedFolder,
								 int initDepth,
								 String origin){
		Log.stdout("Importing corpus from csv "+file);
		Corpus initial = Corpus.fromNodeFile(file,citedFolder);
		Log.stdout("Imported corpus of size "+initial.references.size());

		//update the origin
		for(Reference r:initial){
			//if(orderFile.length()!=0){r.depth=initDepth;} // done later
			r.setOrigin(origin);
		}

		// add citations
		if(citationFile.length()>0) {
			Log.stdout("Adding citations");
			String[][] rawlinks = CSVReader.read(citationFile, ";", "\"");
			Log.stdout("Citation links to import : "+rawlinks.length);
			for (String[] link : rawlinks) {
				String from = link[0];
				String to = link[1];
				// ! do not access Reference.references directly -> use construct instead (done for that)
				//Reference.references.get(to).citing.add(Reference.references.get(from));
				Reference.construct(to).setCiting(Reference.construct(from));
			}
		}


		// add order if needed
		if(orderFile.length()!=0){
			OrderedCorpus order = Corpus.fromCSV(orderFile);
			Log.stdout("Setting depths from order file for "+order.references.size());

			for(int i = 0 ; i<order.orderedRefs.size();i++){
				//order.orderedRefs.get(i).horizontalDepth.put(origin,new Integer(i));
				Log.stdout("hdepth ordered ref "+Integer.toString(i));
				order.orderedRefs.get(i).setHorizontalDepth0(origin,i);
			}

			Log.stdout("horizontalDepth ok");

			// note : set depth only with an order ref file ? yes as assumed as initial layer
			for(int i = 0 ; i<order.orderedRefs.size();i++) {
				Log.stdout("depth ordered ref "+Integer.toString(i));
				order.orderedRefs.get(i).setDepth0(initDepth);
			}

			Log.stdout("depth ok");

			// we assume that in the case if an order file is given, the corpus is full until last depth, so
			// citingFilled can be set to true for all upper levels
			int deepest = Reference.getMinimalDepth();
			for(Reference r:Reference.getReferences()){if (r.getDepth()>deepest){r.setCitingFilled(true);}}
		}else {
			// if no order file is provided, set depth of all refs to initDepth
			for(Reference r:Reference.getReferences()){r.setDepth(initDepth);}
		}

		return(initial);
	}





	
}
