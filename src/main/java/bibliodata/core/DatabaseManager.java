
package bibliodata.core;


import bibliodata.Context;
import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoImport;
import bibliodata.database.mongo.MongoExport;

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
                "| --export $DATABASE $FILE [$MAXPRIORITY] [$WITHABSTRACTS]"
        );}else {

            String action = args[0];

            if (action.equals("--import")) {
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
                if (args.length >= 7) { orderFile = args[6]; }
                if (args.length >= 8) { orderFile = args[7]; }
                MongoImport.fileToMongo(args[1],orderFile,citationFile, "", args[2], Context.getReferencesCollection(), Context.getCitationsCollection(), initDepth, origin, dropCols);
            }

            if (action.equals("--importcit")){
                String citfile = args[1];
                String db = args[2];
                boolean dropCollections = false;
                if(args.length >= 4){dropCollections=Boolean.parseBoolean(args[3]);}
                MongoImport.citationsToMongo(citfile,db,Context.getCitationsCollection(),dropCollections);
            }

            // increase depth
            if (action.equals("--incrdepth")) {
                MongoConnection.initMongo(args[1]);
                MongoConnection.incrAllDepths(Context.getReferencesCollection());
                MongoConnection.closeMongo();
            }

            // set processing to false for all references (to be used after crashes or interrupted runs)
            if (action.equals("--notproc")) {
                MongoConnection.initMongo(args[1]);
                MongoConnection.notProcessing(Context.getReferencesCollection());
                MongoConnection.closeMongo();
            }

            if (action.equals("--priority")) {
                MongoConnection.initMongo(args[1]);
                MongoConnection.computePriorities(Integer.parseInt(args[2]));
                MongoConnection.closeMongo();
            }

            if (action.equals("--export")) {
                MongoConnection.initMongo(args[1]);
                int maxPriority = -1;
                if (args.length >= 4) {
                    maxPriority = Integer.parseInt(args[3]);
                }
                boolean withAbstracts = false;
                if (args.length == 5) {
                    withAbstracts = true;
                }
                MongoExport.export(maxPriority, withAbstracts, args[2]);
                MongoConnection.closeMongo();
            }


        }
    }


}




