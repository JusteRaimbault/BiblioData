/**
 * 
 */
package bibliodata.core;

import java.io.File;
import java.util.HashSet;

import bibliodata.core.AlgorithmicSystematicReview;

import bibliodata.mendeley.MendeleyAPI;
import bibliodata.utils.Log;
import bibliodata.utils.RISReader;
import bibliodata.core.corpuses.CSVFactory;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Reference;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class AbstractSetRetriever {

	/**
	 * 
	 * Fills abstracts from a csv reference file ; and outfile
	 * 
	 * @param args
	 * 
	 * args[0] : infile
	 * args[1] : outfile
	 * 
	 */
	public static void main(String[] args) {
		
		if(args.length != 2){System.out.println("Usage : ... infile outfile");}
		else{

			String refFile = args[0];
			String outFile = args[1];

			AlgorithmicSystematicReview.setup();
			MendeleyAPI.setupAPI();

			Corpus initial = new DefaultCorpus();
			if(refFile.endsWith(".ris")){
				RISReader.read(refFile,-1);
				initial = new DefaultCorpus(Reference.references.keySet());
			}
			if(refFile.endsWith(".csv")){
				initial = new CSVFactory(refFile).getCorpus();
			}
			
			//load out file to get refs already retrieved in a previous run
			Corpus existing = new DefaultCorpus();
			if(new File(outFile).exists()){
				existing = new CSVFactory(outFile).getCorpus();
			}
			
			
			Corpus finalCorpus = new DefaultCorpus();
			
			for(Reference r : initial){
				
				Log.stdout(r.toString());
				if(!existing.references.contains(r)){
					Reference detailed = MendeleyAPI.getReference(r.title.title,r.year,r.scholarID);
					if(detailed!=null){
						finalCorpus.references.add(detailed);
						// export the current corpus
						finalCorpus.csvExport(outFile,true);
					}
				}
			}
			
			// export final corpus csv
			finalCorpus.csvExport(outFile,true);
			
		}
		
	}



	/**
	 * single ref abstract
	 * @param args
	 *
	 * args[0] : title
	 * args[1] : path to config file
	 *
	 */
	/*
	public static void main(String[] args) {
		String title = args[0];
		String confPath = args[1];

		// do not forget to setup api

		AlgorithmicSystematicReview.setup(confPath);

		MendeleyAPI.setupAPI();

		// rq : replacement should not been needed as provided title will be already treated (in appscript ?)
		HashSet<Reference> refs = MendeleyAPI.catalogRequest(title.replaceAll(" ","+").replaceAll("\\{", "").replaceAll("\\}", ""), 1,false);
		//at most one element
		Reference r = refs.iterator().next();
		String qTitle = StringUtils.lowerCase(title.replaceAll("\\+", " ").replaceAll("\\{", "").replaceAll("\\}", ""));
		String rTitle = StringUtils.lowerCase(r.title.title);

		try{
			if(StringUtils.getLevenshteinDistance(qTitle,rTitle)< 4){
				System.out.println(r.resume);
			}
			//else do nothing, no abstract found
		}catch(Exception e){
			//empty abstract if issue

		}

	}
	*/




}
