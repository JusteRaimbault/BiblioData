
package bibliodata.core;


import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoImport;

public class DatabaseManager {


    public static void main(String[] args){

        if(args.length==0){System.out.println("Usage : --database | --import $FILE $DATABASE [$DEPTH] | --incrdepth $DATABASE");}

        String action = args[0];

        if(action.equals("--import")){
            int initDepth = 0;
            boolean dropCols = false;
            if(args.length>=4){initDepth=Integer.parseInt(args[3]);}
            if(args.length>=5){dropCols=Boolean.parseBoolean(args[4]);}
            MongoImport.fileToMongo(args[1],"",args[2],"references","citations",initDepth,dropCols);
        }

        if(action.equals("--incrdepth")){
            MongoConnection.initMongo(args[1]);
            MongoConnection.incrAllDepths("references");
        }


    }


}




