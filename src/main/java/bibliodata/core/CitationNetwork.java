/**
 * 
 */
package bibliodata.core;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import bibliodata.core.corpuses.CSVFactory;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Reference;
import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoDocument;
import bibliodata.scholar.ScholarAPI;
import bibliodata.database.sql.CybergeoImport;
import bibliodata.database.sql.SQLConnection;
import bibliodata.utils.CSVWriter;
import bibliodata.utils.GEXFWriter;
import bibliodata.utils.Log;
import bibliodata.utils.RISReader;
import bibliodata.utils.tor.TorPool;
import bibliodata.utils.tor.TorPoolManager;

/**
 * 
 * Class to extract partial citation network from existing ref files
 * 
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class CitationNetwork {


	/**
	 * Construct the citation network for refs in a mongo database
	 * @param database
	 * @param refcollection
	 * @param linkcollection
	 * @param numrefs
	 */
	public static void fillCitationsMongo(String database,String refcollection,String linkcollection,int numrefs,int maxPriority){

		TorPoolManager.setupTorPoolConnexion(true,true);
		ScholarAPI.init();

		Log.stdout("Filling mongobase "+database+" ; collections "+refcollection+","+linkcollection+" ; "+numrefs+" refs");

		//init mongo connection
		MongoConnection.initMongo(database);

		for(int i = 0;i<numrefs;i++){
			Reference r = MongoConnection.getUnfilled(refcollection,maxPriority);
			if(r==null){break;}
			Log.stdout("Unfilled ref : "+r.toString());
			ScholarAPI.fillIdAndCitingRefs(new DefaultCorpus(r));
			MongoConnection.updateCorpus(new DefaultCorpus(r,r.citing),refcollection,linkcollection);
		}

		// corpus need to be updated at each loop to iterate on depth !
		//MongoConnection.updateCorpus(new DefaultCorpus(Reference.references.keySet()),refcollection,linkcollection);

		MongoConnection.closeMongo();
	}

	
	/**
	 * Build first order network : foreach ref, find citing refs
	 */
	public static void buildCitationNetwork(String outFile,Corpus existing){
		Corpus base = new DefaultCorpus(Reference.references.keySet());
		
		for(Reference r:base){
			if(!existing.references.contains(r)){
			  ScholarAPI.fillIdAndCitingRefs(new DefaultCorpus(r));
			  // export
			  new DefaultCorpus(Reference.references.keySet()).csvExport(outFile,false);
			}
		}
	}
	

	
	
	/**
	 * Given a RIS ref file, builds its corresponding citation network.
	 */
	public static void buildCitationNetworkFromRefFile(String refFile,String outFile,int depth,String citedFolder){
        
		//AlgorithmicSystematicReview.setup("conf/default.conf");

		try{TorPoolManager.setupTorPoolConnexion(true);}catch(Exception e){e.printStackTrace();}
		ScholarAPI.init();
		
		System.out.println("Reconstructing References from file "+refFile);
		
		Corpus initial = Corpus.fromNodeFile(refFile,citedFolder);

		System.out.println("Number of References : "+Reference.references.keySet().size());

		//load out file to get refs already retrieved in a previous run
		Corpus existing = new DefaultCorpus();
		if(new File(outFile).exists()){
			existing = new CSVFactory(outFile).getCorpus();
		}

		System.out.println("Already got : "+existing.references.size());

		//System.out.println("Initial Refs : ");for(Reference r:Reference.references.keySet()){System.out.println(r.toString());}
		
		for(int d=1;d<=depth;d++){
		  System.out.println("Getting Citation Network, depth "+d);
		  buildCitationNetwork(outFile,existing);
		}

		for(Reference r:Reference.references.keySet()){System.out.println(r.toString());}

		/*
		System.out.println("Getting Abstracts...");
		MendeleyAPI.setupAPI();
		for(Reference r:Reference.references.keySet()){
			System.out.println(r.title.replace(" ", "+"));
			//MendeleyAPI.catalogRequest(r.title.replace(" ", "+"), 1);
		}
		*/
		
		//GEXFWriter.writeCitationNetwork(outFile, Reference.references.keySet());
		
		//TorPool.stopPool();
	}
	
	
	/**
	 * Build the network 
	 */
	public static void buildCitationNetworkFromSQL(String outFile){
		
		//AlgorithmicSystematicReview.setup("conf/default.conf");

		TorPool.setupConnectionPool(50,false);
		ScholarAPI.init();
		
		//import database
		System.out.println("Setting up from sql...");
		SQLConnection.setupSQL("Cybergeo");
		Set<Reference> initialRefs = CybergeoImport.importBase("WHERE  `datepubli` >=  '2003-01-01' AND  `resume` !=  '' AND  `titre` != ''");
		System.out.println("References :  : "+Reference.references.size());
		
		
		// construct network
		buildCitationNetwork("",new DefaultCorpus());
		
        GEXFWriter.writeCitationNetwork(outFile, Reference.references.keySet());
		
		TorPool.stopPool();
		
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		buildCitationNetworkFromRefFile("../../../Reflexivity/data/test.bib","../../../Reflexivity/data/testout",1,"");
		
		//TorPool.forceStopPID(1971, 2020);
		
		//buildCitationNetworkFromSQL("res/citation/cybergeo.gexf");
		
		//buildCitationNetworkFromRefFile("/Users/Juste/Documents/ComplexSystems/Cybergeo/Data/processed/2003_fullbase_rawTitle.ris","res/citation/cybergeo_depth2.gexf",2);
		
		//buildCitationNetworkFromRefFile("/Users/Juste/Documents/ComplexSystems/Cybergeo/Data/processed/2003_frenchTitles_fullbase.ris","res/citation/cybergeo.gexf");
		
		/*
		//String[] keywords = {"land+use+transport+interaction","city+system+network","network+urban+modeling","population+density+transport","transportation+network+urban+growth","urban+morphogenesis+network"};
		
		String[] keywords = {"land+use+transport+interaction"};
		
		buildGeneralizedNetwork(
				"/Users/Juste/Documents/ComplexSystems/CityNetwork/Models/Biblio/AlgoSR/cit/refs_",
				keywords,
				"res/citation/citations",
				20);
				
		*/
	}

}
