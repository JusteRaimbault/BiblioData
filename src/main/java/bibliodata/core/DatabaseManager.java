
package bibliodata.core;


import bibliodata.Context;
import bibliodata.database.mongo.MongoCommand;
import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoImport;
import bibliodata.database.mongo.MongoExport;
import bibliodata.utils.CSVReader;
import bibliodata.utils.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

public class DatabaseManager {


    public static void main(String[] args){

        if(args.length==0){
            System.out.println(
                "Usage : --database\n"+
                "| --import $FILE $DATABASE [$DEPTH] [$ORIGIN] [$DROP_COLLECTIONS] [$ORDERFILE] [$CITATIONFILE]\n"+
                "| --importcit $CITFILE $DATABASE [$DROP_COLLECTIONS]\n"+
                "| --incrdepth $DATABASE\n"+
                "| --notproc $DATABASE\n"+
                "| --priority $DATABASE $MAXDEPTH\n"+
                "| --timestamp $DATABASE $TIMESTAMP\n"+
                "| --export $DATABASE $FILE [$MAXPRIORITY] [$MAXDEPTH] [$WITHABSTRACTS]\n"+
                "| --exportconso $FILE $MAXPRIORITY $MAXDEPTH $D1 $D2 ... \n"+
                "| --consolidate $DB1 ... $DBN"
        );}else {

            String action = args[0];

            if (action.equals("--import")) {

                if (args.length==1) {
                    // print usage
                    System.out.println(
                       "Import corpus from csv to mongo. Usage : --database --import\n"+
                       "  $FILE : path to csv file with corpus to import, required format at least 'title;id', optional year third column\n"+
                       "        (id = \"NA\" will self generate ids)"+
                       "  $DATABASE : name of database where to import \n"+
                       "  [$DEPTH] (optional) : depth of higher layer \n"+
                       "  [$ORIGIN] (optional) : name of corpus origin \n"+
                       "  [$DROP_COLLECTIONS] (optional) : boolean, should collections in the database be dropped (default to false) \n"+
                       "  [$ORDERFILE] (optional) : path to csv file with ordered higher layer (used to compute horizontal depth) \n"+
                       "  [$CITATIONFILE] (optional) : path to csv link file (format 'from;to') "+
                       "\n\n"+
                       "To import corpus of the form oneref <- manyrefs in files: preprocess to create full corpus file with NAs ids and link file; use --import and --importcit"
                    );
                }else {

                    int initDepth = 0;
                    boolean dropCols = false;
                    String origin = "";
                    String orderFile = "";
                    String citationFile = "";
                    if (args.length >= 4) {
                        initDepth = Integer.parseInt(args[3]);
                    }
                    if (args.length >= 5) {
                        origin = args[4];
                    }
                    if (args.length >= 6) {
                        dropCols = Boolean.parseBoolean(args[5]);
                    }
                    if (args.length >= 7) {
                        orderFile = args[6];
                    }
                    if (args.length >= 8) {
                        citationFile = args[7];
                    }
                    MongoImport.fileToMongo(args[1], orderFile, citationFile, "", args[2], Context.getReferencesCollection(), Context.getCitationsCollection(), initDepth, origin, dropCols);
                }
            }

            if (action.equals("--importcit")){
                if (args.length==1){
                    System.out.println("Import citation links to mongo. Usage --database --importcit \n"+
                            "  $CITFILE : path to csv citation file (format 'from;to') \n"+
                            "  $DATABASE : name of database where to import \n"+
                            "  [$DROP_COLLECTIONS] (optional) : should citation collection be dropped (defaults to false)"
                            );
                }else {
                    String citfile = args[1];
                    String db = args[2];
                    boolean dropCollections = false;
                    if (args.length >= 4) {
                        dropCollections = Boolean.parseBoolean(args[3]);
                    }
                    MongoImport.citationsToMongo(citfile, db, Context.getCitationsCollection(), dropCollections);
                }
            }

            // increase depth
            if (action.equals("--incrdepth")) {
                if (args.length==1){
                    System.out.println("Increment depth of all references in the database. Usage : --database --incrdepth \n"+
                            "  $DATABASE : name of the database"
                            );
                }else {
                    MongoConnection.initMongo(args[1]);
                    MongoCommand.incrAllDepths(Context.getReferencesCollection());
                    MongoConnection.closeMongo();
                }
            }

            // set processing to false for all references (to be used after crashes or interrupted runs)
            if (action.equals("--notproc")) {
                if (args.length==1) {
                    System.out.println("Set processing to false. Usage : --database --notproc \n"+
                            "  $DATABASE : name of the database"
                            );
                }else {
                    MongoConnection.initMongo(args[1]);
                    MongoCommand.notProcessing(Context.getReferencesCollection());
                    MongoConnection.closeMongo();
                }
            }

            if (action.equals("--priority")) {
                if (args.length==1){
                    System.out.println("Compute priorities. Usage : --database --priority \n"+
                            "  $DATABASE : name of the database \n"+
                            "  $MAXDEPTH : maximal depth at which compute (starting from origin layer, so minimal depth indeed)"
                            );
                }else {
                    MongoConnection.initMongo(args[1]);
                    MongoCommand.computePriorities(Integer.parseInt(args[2]));
                    MongoConnection.closeMongo();
                }
            }

            if (action.equals("--timestamp")) {
                if (args.length==1){
                    System.out.println("Adding timestamp. Usage : --database --timestamp \n"+
                            "  $DATABASE : name of the database \n"+
                            "  $TIMESTAMP : long ts or 'now'"
                            );
                } else {
                    long ts;
                    if (args[2].equals("now")){ts = Log.currentTimestamp();}else {ts = Long.parseLong(args[2]);}
                    MongoConnection.initMongo(args[1]);
                    MongoCommand.addTimeStamp(ts,Context.getReferencesCollection());
                    MongoConnection.closeMongo();
                }
            }

            if (action.equals("--export")) {
                if(args.length<=2) {
                    System.out.println(
                            "Export database to csv. Usage : --database --export \n"+
                            "  $DATABASE : name of database \n"+
                            "  $FILE : file prefix \n"+
                            "  [$MAXPRIORITY] (optional): max priority to export (-1 for all) \n"+
                            "  [$MAXDEPTH] (optional): max vertical depth (-1 for all) \n"+
                            "  [$INITDEPTH] (optional): init layer vertical depth (from which horizontal depth is propagated) \n"+
                            "  [$FILTERFILE] (optional): csv file (first column id or prefix of id) to filter references (delim ;, quote \") - false for no filter\n"+
                            "  [$FILTEREXCLUSIVE] (optional): boolean to have an exclusive filtering (true) or not - false by default \n"+
                            "  [$KEPTHDEPTHS] (optional): string giving the horizontal depth labels to be kept ( ; separator) \n"+
                            "  [$NUMREFS] (optional): number of references (-1 for all) \n"+
                            "  [$WITHABSTRACTS] (optional): export abstracts if exist (false by default)"
                            );
                }else {
                    String database = args[1];
                    String fileprefix = args[2];
                    MongoConnection.initMongo(database);
                    int maxPriority = -1;
                    if (args.length >= 4) {
                        maxPriority = Integer.parseInt(args[3]);
                    }
                    int maxDepth = -1;
                    if (args.length >= 5) {
                        maxDepth = Integer.parseInt(args[4]);
                    }
                    int initLayerDepth = 0;
                    if (args.length >= 6) {
                        initLayerDepth = Integer.parseInt(args[5]);
                    }
                    HashSet<String> filter = new HashSet<>();
                    if (args.length >= 7) {
                        String filterFile = args[6];
                        if (!filterFile.equals("false")) {
                            String[][] tofilter = CSVReader.read(filterFile, ";", "\"");
                            if (tofilter!=null) {
                                for (String[] row : tofilter) {
                                    String filteredid = row[0];
                                    //add-hoc removal of trailing 0s (issue formatting)
                                    while (filteredid.endsWith("0")) {
                                        filteredid = filteredid.substring(0, filteredid.length() - 1);
                                    }
                                    filter.add(filteredid);
                                }
                            }
                        }
                    }
                    boolean filterExclusive = false;
                    if (args.length >= 8) {
                        filterExclusive =  Boolean.parseBoolean(args[7]);
                    }
                    HashSet<String> keptHorizontalDepths = new HashSet<>();
                    if (args.length >= 9) {
                        String[] kept = args[8].replace("\"","").split(";");
                        for(String k: kept){System.out.println("kept hdepth : "+k);}
                        keptHorizontalDepths.addAll(Arrays.asList(kept));
                    }
                    int numRefs = -1;
                    if (args.length >= 10) {
                        numRefs = Integer.parseInt(args[9]);
                    }
                    boolean withAbstracts = false;
                    if (args.length == 11) {
                        withAbstracts = Boolean.parseBoolean(args[10]);
                    }
                    MongoExport.export(maxPriority, maxDepth,initLayerDepth,numRefs, withAbstracts, filter, filterExclusive, keptHorizontalDepths, fileprefix);
                    MongoConnection.closeMongo();
                }
            }

            if (action.equals("--exportconso")){
                if (args.length==1){
                    System.out.println("Export consolidated from different databases. Usage : --database --exportconso \n"+
                            "  $FILE : file prefix \n"+
                            "  $MAXPRIORITY : max priority \n"+
                            "  $MAXDEPTH : max depth \n "+
                            "  $D1 $D2 ... : any number of database names"
                            );
                }else {
                    if (args.length < 5) {
                        Log.stdout("No db to export");
                    } else {
                        String file = args[1];
                        int maxPriority = Integer.parseInt(args[2]);
                        int maxDepth = Integer.parseInt(args[3]);
                        LinkedList<String> dbs = new LinkedList<>();
                        for (int i = 4; i < args.length; i++) {
                            dbs.add(args[i]);
                        }

                        MongoExport.exportConsolidated(maxPriority, maxDepth, dbs, file);
                    }
                }

            }

            if (action.equals("--consolidate")){
                if (args.length==1){
                    System.out.println("Consolidate databases into a single database (by default 'consolidated'). Usage : --database --consolidate \n"+
                            "   $DB1 ... $DBN : database to consolidate"
                            );
                }else {
                    LinkedList<String> dbs = new LinkedList<>();
                    for (int i = 1; i < args.length; i++) {
                        dbs.add(args[i]);
                    }
                    MongoCommand.consolidate(dbs,-2);
                }
            }


        }
    }


}




