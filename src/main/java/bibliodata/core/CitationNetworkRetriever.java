/**
 * 
 */
package bibliodata.core;

import bibliodata.Context;
import bibliodata.utils.Log;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class CitationNetworkRetriever {

	/**
	 * @param args
	 *
	 */
		public static void main(String[] args) {

			if(args.length==0){
				System.out.println("Usage : --citation\n"+
					"| --mongo $DATABASE $NUMREFS [$MAXPRIORITY] [$CONSOLIDATIONDATABASE]\n"+
					"| --csv $CSVFILE $OUTPUT_PREFIX $DEPTH [$CITEDFOLDER]"
			);}

			String action = args[0];

			if(action.equals("--mongo")){
			    if (args.length == 1){
			        System.out.println("Constructing citation network from mongo. Usage : --citation --mongo \n"+
                            "  $DATABASE : name of the database \n"+
                            "  $NUMREFS : number of references for which to collect citations \n"+
                            "  [$MAXPRIORITY] (optional) : maximal priority at which references are queried - the priority field must be filled (with database manager after kw req e.g.)\n"+
                            "  [$CONSOLIDATION] (optional) : if true, default central db, otherwise name of consolidation database\n"+
                            "  [$CONSOONLY] (optional) : if true, does not attempt scholar requests"
                            );
                }else {
                    Log.stdout("Citation network from mongo");
                    String mongodb = args[1];
                    //String collection = args[2];
                    String refcollection = Context.getReferencesCollection();
                    String linkcollection = Context.getCitationsCollection();
                    int numrefs = Integer.parseInt(args[2]);
                    //int maxPriority = Context.getMaxHorizontalDepth(); // for corpuses where priority is not defined, better set to -1
                    int maxPriority = -1;
                    String consolidationDatabase = "";
                    boolean consoonly = false;
                    if (args.length >= 4) {
                        maxPriority = Integer.parseInt(args[3]);
                    }
                    if (args.length >= 5) {
                        if (args[4].equals("true")){consolidationDatabase = Context.getCentralDatabase();}
                        else {consolidationDatabase = args[4];}
                    }
                    if (args.length == 6) {
                        consoonly = Boolean.parseBoolean(args[5]);
                    }
                    CitationNetwork.fillCitationsMongo(mongodb, refcollection, linkcollection, numrefs, maxPriority,consolidationDatabase,consoonly);
                }
			}


			/**
			 * Usage : reffile,outfile,depth,[citedfolder]
			 */
			if(action.equals("--csv")) {

			    if (args.length==1){
			        System.out.println("Constructing citation network from csv. Usage : --citation --csv \n"+
                            "  $CSVFILE : initial corpus file \n"+
                            "  $OUTPUT_PREFIX : prefix for output file \n"+
                            "  $DEPTH : up to which depth go \n"+
                            "  [$CITEDFOLDER] (optional) : folder containing references bibliographies"
                            );
                }else {

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
                        //System.out.println("usage : java -jar citationNetwork.jar reffile outfile depth [cited] TEST");
                    }

                    CitationNetwork.buildCitationNetworkFromRefFile(refFile, outFile, depth, citedFolder);
                }
			}

		}

}
