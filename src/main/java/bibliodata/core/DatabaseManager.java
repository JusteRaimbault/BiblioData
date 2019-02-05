
package bibliodata.core;


import bibliodata.database.mongo.MongoImport;

public class DatabaseManager {


    public static void main(String[] args){

        if(args.length==0){System.out.println("Usage : --database --import $FILE $DATABASE [$DEPTH]");}

        String action = args[0];

        if(action.equals("--import")){
            int initDepth = 0;
            if(args.length>=4){initDepth=Integer.parseInt(args[3]);}
            MongoImport.fileToMongo(args[1],"",args[2],"references","citations",initDepth);
        }


    }


}




