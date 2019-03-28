
package bibliodata.core;


import bibliodata.Context;
import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoImport;

public class DatabaseManager {


    public static void main(String[] args){

        if(args.length==0){
            System.out.println(
                "Usage : --database\n"+
                "| --import $FILE $DATABASE [$DEPTH] [$ORIGIN] [$DROP_COLLECTIONS]\n"+
                "| --incrdepth $DATABASE\n"+
                "| --notproc $DATABASE"
        );}

        String action = args[0];

        if(action.equals("--import")){
            int initDepth = 0;
            boolean dropCols = false;
            String origin ="";
            if(args.length>=4){initDepth=Integer.parseInt(args[3]);}
            if(args.length>=5){origin=args[4];}
            if(args.length>=6){dropCols=Boolean.parseBoolean(args[5]);}
            MongoImport.fileToMongo(args[1],"",args[2], Context.getReferencesCollection(),Context.getCitationsCollection(),initDepth,origin,dropCols);
        }

        // increase depth
        if(action.equals("--incrdepth")){
            MongoConnection.initMongo(args[1]);
            MongoConnection.incrAllDepths(Context.getReferencesCollection());
        }

        // set processing to false for all references (to be used after crashes or interrupted runs)
        if(action.equals("--notproc")){
            MongoConnection.initMongo(args[1]);
            MongoConnection.notProcessing(Context.getReferencesCollection());
        }


    }


}




