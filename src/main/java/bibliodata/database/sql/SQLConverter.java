/**
 * 
 */
package bibliodata.database.sql;

import java.util.HashSet;

import bibliodata.core.AlgorithmicSystematicReview;
import bibliodata.utils.CSVWriter;
import bibliodata.utils.GEXFWriter;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Reference;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class SQLConverter {
	
	
	/**
	 * converts a simple cyb sql base to gexf.
	 * 
	 * @param database
	 * @param outfile
	 */
	public static void sqlToGexf(String database,String outfile){
		Corpus corpus = SQLImporter.sqlImport(database, "cybergeo", "refs", "links",-1, false);
		GEXFWriter.writeCitationNetwork(outfile, corpus);
	}
	
	/**
	 * converts sql to csv (links and nodes)
	 * 
	 * @param database
	 * @param outPrefix
	 */
	public static void sqlToCsv(String database, String outPrefix){
		Corpus corpus = SQLImporter.sqlImport(database, "cybergeo", "refs", "links", -1,false);
		
		HashSet<String[]> links = new HashSet<String[]>();
		HashSet<String[]> refs = new HashSet<String[]>();
		
		for(Reference r:corpus){
			String prim = "0";if(r.getAttributes().containsKey("primary")){prim="1";}
			String year="1000";if(r.getYear().length()>0){year=r.getYear();}
			String[] ref = {r.getId(),r.getTitle().title,year,prim};
			// check if title formatting causes a pb in csv import -> only ID and prim
			//String[] ref = {r.scholarID,prim};
			refs.add(ref);
			for(Reference rc:r.getCiting()){String[] link = {rc.getId(),r.getId()};links.add(link);}
		}
		
		String[][] linksTab = new String[links.size()][2];
		String[][] refsTab = new String[refs.size()][4];
		
		int i = 0;for(String[] l:links){linksTab[i]=l;i++;}
		i = 0;for(String[] l:refs){refsTab[i]=l;i++;}
		
		CSVWriter.write(outPrefix+"_edges.csv", linksTab, "\t","");
		CSVWriter.write(outPrefix+"_nodes.csv", refsTab, "\t","");
	}
	
	
	public static void main(String[] args){
		//FIXME sql setup
		//AlgorithmicSystematicReview.setup();
		sqlToCsv("cybfull","res/nwcsv/full");
		//sqlToGexf("cybfull","/Users/Juste/Documents/ComplexSystems/Cybergeo/cybergeo20/CitationNetwork/Data/nw/full.gexf");
	}
	
	
	
}
