/**
 *
 */
package bibliodata.core;

import java.util.List;

import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.OrderedCorpus;
import bibliodata.core.reference.Reference;
import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoImport;
import bibliodata.scholar.ScholarAPI;
import bibliodata.utils.CSVReader;
import bibliodata.utils.Log;
import bibliodata.utils.proxy.TorPoolManager;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class KeywordsRequest {


	/**
	*  Construct a corpus from keyword request
	 *   Keyword file specification :
	 *    - csv with delimiter ";"
	 *    - if one column, terms separated by space are merge with "+" (equivalent to a "AND" in the request)
	 *    - if two columns, idem, but first column is the corpus identifier
	*/
	public static void main(String[] args) {

		if(args.length==0){
			System.out.println(
					"Usage : --keywords\n"+
							"| --file $KWFILE $OUTFILE $NUMREF [$ADDTERM]\n"+
							"| --mongo $KWFILE $DATABASE $NUMREF [$INITDEPTH] [$DROPCOLLECTION] [$ADDTERM]"
			);
		}

		String mode = args[0];


		if (args.length==1){

			System.out.println("Construct corpus from file.");

			if (mode.equals("--file")){
				System.out.println("Usage : --keywords --file \n"+
						"  $KWFILE : file with keyword requests (format 'request;requestcode') \n"+
						"  $OUTFILE : file to store resulting corpus \n"+
						"  $NUMREF : number of references to collect for each kw \n"+
						"  [$ADDTERM] (optional) : additional term to append to each request; request is wrapped with quotes"
						);
			}

			if (mode.equals("--mongo")){
				System.out.println("Usage : --keywords --mongo \n"+
						"  $KWFILE : file with keyword requests (format 'request;requestcode') \n"+
						"  $DATABASE : name of database in which resulting corpus is stored \n"+
						"  $NUMREF : number of references to collect for each kw \n"+
						"  [$INITDEPTH] (optional) : depth of the initial layer \n"+
						"  [$DROPCOLLECTION] (optional) : should reference collection be dropped \n"+
						"  [$ADDTERM] (optional) : additional term to append to each request; request is wrapped with quotes"
						);
			}

		}else {


			if ((args.length == 4 || args.length == 5 || args.length == 6 || args.length == 7) && (mode.equals("--file") || mode.equals("--mongo"))) {


				String kwFile = args[1];
				String out = args[2];// either the file or the database
				int numref = Integer.parseInt(args[3]);
				int initdepth = 0;
				if (args.length == 5) {
					initdepth = Integer.parseInt(args[4]);
				}
				boolean dropcols = false;
				if (args.length == 6) {
					dropcols = Boolean.parseBoolean(args[5]);
				}
				String addterm = "";
				if (args.length == 7) {
					addterm = args[6];
				}

				if (mode.equals("--file")) {
					TorPoolManager.setupTorPoolConnexion(true);
				} else {
					TorPoolManager.setupTorPoolConnexion(true, true);
				}

				// fix: as the scholar request uses mongo to log IPs, needs to be already initialized here
				if (mode.equals("--mongo")) MongoConnection.initMongo(out);

				ScholarAPI.init();

				// parse kws file
				String[][] kwraw = CSVReader.read(kwFile, ";", "\"");
				//System.out.println(kwraw.length);
				String[] reqs = new String[kwraw.length];
				String[] reqnames = new String[kwraw.length];
				for (int i = 0; i < kwraw.length; i++) {
					String currentreq = "";
					if (kwraw[i].length > 1) {
						currentreq = kwraw[i][1].replace(" ", "+");
						reqnames[i] = kwraw[i][0];
					} else {
						currentreq = kwraw[i][0].replace(" ", "+");
						reqnames[i] = kwraw[i][0].replace(" ", "_");
					}
					if (addterm.length() > 0) {
						currentreq = "\""+currentreq+"\""+ "+" + addterm;
					}
					reqs[i] = currentreq;

					Log.stdout("Request : " + currentreq);
				}


				//try{(new FileWriter(new File(outFile+"_achieved.txt"))).write(kwFile+'\n');}catch(Exception e){e.printStackTrace();}

				for (int i = 0; i < reqs.length; i++) {
					String req = reqs[i];
					String reqname = reqnames[i];

					Log.stdout("Keyword request \"" + req + "\" for " + numref + " refs");

					List<Reference> currentrefs = ScholarAPI.scholarRequest(req, numref, "direct");

					// add additional info
					int hdepth = 0;
					for (Reference r : currentrefs) {
						r.setDepth(initdepth);
						r.setOrigin(reqname);
						r.setHorizontalDepth(reqname, new Integer(hdepth));
						hdepth++;
					}

					Corpus toexport = new OrderedCorpus(currentrefs);

					if (mode.equals("--file")) {
						toexport.csvExport(out + "_" + req, false);
					}

					if (mode.equals("--mongo")) {
						boolean drop = false;
						if (i == 0 && dropcols) {
							drop = true;
						}// drop only at the first kw
						//Log.stdout("to export : "+toexport.references.size());
						//for(Reference r:toexport.references){Log.stdout(r.title.title);for(String h:r.horizontalDepth.keySet()){Log.stdout(h+" -> "+r.horizontalDepth.get(h));}}
						// exporting kwreq by kwreq causes a redundancy with horizdepth when names are duplicated -> change merge strategy with latest only
						MongoImport.corpusToMongo(toexport, out, "references", "links", drop);
					}

					// write kws in achieved file
					//try{(new FileWriter(new File(outFile+"_achieved.txt"),true)).write(req+"\n");}catch(Exception e){e.printStackTrace();}
				}


			}


		}

	}

}
