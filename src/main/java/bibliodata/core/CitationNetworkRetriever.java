/**
 * 
 */
package bibliodata.core;

import bibliodata.utils.Log;
import bibliodata.utils.tor.TorPool;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class CitationNetworkRetriever {

	/**
	 * @param args
	 * 
	 * Usage : [torpid1,torpid2] , refFile, outFile, depth
	 * 
	 */
		public static void main(String[] args) {

			if(args.length==0){System.out.println("Usage : --citation --mongo/--csv");}

			String action = args[0];

			/**
			 * Usage :
			 */
			if(action.equals("--mongo")){
				Log.stdout("Citation network from mongo");
				String mongodb = args[1];
				//String collection = args[2];
				String refcollection = "references";
				String linkcollection = "links";
				int numrefs = Integer.parseInt(args[2]);
				CitationNetwork.fillCitationsMongo(mongodb,refcollection,linkcollection,numrefs);
			}


			/**
			 * Usage : reffile,outfile,depth,[citedfolder]
			 */
			if(action.equals("--csv")) {

				String refFile = "", outFile = "";
				int depth = 0;
				String citedFolder = "";

				/*
				if(args.length==5){
				   TorPool.forceStopPID(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
			 	  refFile = args[2];outFile = args[3];
				   depth = Integer.parseInt(args[4]);
				}
				*/

				if (args.length == 3) {
					refFile = args[0];
					outFile = args[1];
					depth = Integer.parseInt(args[2]);
				} else {
					if (args.length == 4) {
						refFile = args[0];
						outFile = args[1];
						depth = Integer.parseInt(args[2]);
						citedFolder = args[3];
					}
					// print usage
					System.out.println("usage : java -jar citationNetwork.jar reffile outfile depth [cited] TEST");
				}

				CitationNetwork.buildCitationNetworkFromRefFile(refFile, outFile, depth, citedFolder);
			}

		}

}
