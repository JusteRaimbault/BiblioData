/**
 * 
 */
package bibliodata.core;

import java.io.File;
import java.util.HashSet;

import bibliodata.Context;
import bibliodata.core.AlgorithmicSystematicReview;

import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoReference;
import bibliodata.mendeley.MendeleyAPI;
import bibliodata.scholar.ScholarAPI;
import bibliodata.utils.Log;
import bibliodata.utils.RISReader;
import bibliodata.core.corpuses.CSVFactory;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Reference;
import com.mongodb.client.FindIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import static com.mongodb.client.model.Filters.*;

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
		
		if(args.length == 0){
			System.out.println("Usage --abstracts \n"+
					" | --file $INFILE $OUTFILE \n"+
					" | --mongo $DATABASE $NUMREFS");
		}
		else{

			MendeleyAPI.setupAPI("conf/mendeley");

			if(args[0].equals("--file")) {

				String refFile = args[0];
				String outFile = args[1];

				Corpus initial = new DefaultCorpus();
				if (refFile.endsWith(".ris")) {
					RISReader.read(refFile, -1);
					initial = new DefaultCorpus(Reference.getReferences());
				}
				if (refFile.endsWith(".csv")) {
					initial = new CSVFactory(refFile).getCorpus();
				}

				//load out file to get refs already retrieved in a previous run
				Corpus existing = new DefaultCorpus();
				if (new File(outFile).exists()) {
					existing = new CSVFactory(outFile).getCorpus();
				}

				Corpus finalCorpus = new DefaultCorpus();

				for(Reference r : initial){

					Log.stdout(r.toString());
					if(!existing.references.contains(r)){
						Reference detailed = MendeleyAPI.getReference(r.getTitle().title,r.getYear(),r.getId());
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

			
			if(args[0].equals("--mongo")){

				String database = args[1];
				int numrefs = Integer.parseInt(args[2]);
				String refcollection = "references";

				MongoConnection.initMongo(database);

				for(int i = 0;i<numrefs;i++){
					//Reference r = MongoReference.getUnfilled(refcollection, Context.getMaxHorizontalDepth());
					FindIterable res = MongoConnection.getCollection(refcollection).find(or(not(exists("mendeleyChecked")),eq("mendeleyChecked","false")));
					if(!res.iterator().hasNext()){break;}
					else {
						Document d = (Document) res.iterator().next();
						Reference r = Reference.construct(d.getString("id"), d.getString("title"));
						Log.stdout("Unfilled ref for mendeley data: " + r.toString());
						Reference detailed = MendeleyAPI.getReference(r.getTitle().title, r.getYear(), r.getId());
						if (detailed != null) {
							Log.stdout("Obtained: "+detailed.toString());
							detailed.setAttribute("mendeleyChecked","true");
							MongoReference.updateReference(detailed, refcollection, false);
						}
					}
				}

				// corpus need to be updated at each loop to iterate on depth !
				//MongoConnection.updateCorpus(new DefaultCorpus(Reference.references.keySet()),refcollection,linkcollection);

				MongoConnection.closeMongo();

			}



			
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
