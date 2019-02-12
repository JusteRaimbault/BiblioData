
package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;

import org.bson.Document;

import java.util.List;

public class MongoImport {


    public static void fileToMongo(String file,String citedFolder,String db, String refcollection, String citcollection,int initDepth,boolean dropCollections){
        Corpus initial = Corpus.fromFile(file,citedFolder);
        Log.stdout("Imported corpus of size "+initial.references.size());
        //update the depth
        for(Reference r:initial){r.depth=initDepth;}
        MongoConnection.initMongo(db);
        List<Document> toinsert = MongoDocument.fromCorpus(initial);
        Log.stdout("To import : "+toinsert.size());
        if(dropCollections){
            MongoConnection.mongoDatabase.getCollection(refcollection).drop();
            MongoConnection.mongoDatabase.getCollection(citcollection).drop();
        }
        MongoConnection.mongoInsert(refcollection,toinsert);
        MongoConnection.mongoInsert(citcollection,MongoDocument.citationLinksFromCorpus(initial));
        MongoConnection.closeMongo();
    }





}
