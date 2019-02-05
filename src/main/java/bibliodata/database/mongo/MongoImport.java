
package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.utils.Log;

import org.bson.Document;

import java.util.List;

public class MongoImport {


    public static void fileToMongo(String file,String citedFolder,String db, String refcollection, String citcollection,int initDepth){
        Corpus initial = Corpus.fromFile(file,citedFolder);
        Log.stdout("Imported corpus of size "+initial.references.size());
        MongoConnection.initMongo(db);
        List<Document> toinsert = MongoDocument.fromCorpus(initial,initDepth);
        Log.stdout("To import : "+toinsert.size());
        MongoConnection.mongoInsert(refcollection,toinsert);
        MongoConnection.mongoInsert(citcollection,MongoDocument.citationLinksFromCorpus(initial));
        MongoConnection.closeMongo();
    }





}
