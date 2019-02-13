
package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;

import org.bson.Document;

import java.util.List;

public class MongoImport {


    public static void fileToMongo(String file,String citedFolder,String db, String refcollection, String citcollection,int initDepth,String origin,boolean dropCollections){
        Corpus initial = Corpus.fromFile(file,citedFolder);
        Log.stdout("Imported corpus of size "+initial.references.size());
        //update the depth
        for(Reference r:initial){
            r.depth=initDepth;
            r.origin=origin;
        }
        MongoConnection.initMongo(db);

        //List<Document> toinsert = MongoDocument.fromCorpus(initial);

        Log.stdout("To import : "+initial.references.size());

        if(dropCollections){
            MongoConnection.mongoDatabase.getCollection(refcollection).drop();
            MongoConnection.mongoDatabase.getCollection(citcollection).drop();
        }

        //MongoConnection.mongoInsert(refcollection,toinsert);
        //MongoConnection.mongoInsert(citcollection,MongoDocument.citationLinksFromCorpus(initial));
        // better updating to avoid duplicates also in initial import
        MongoConnection.updateCorpus(initial,refcollection,citcollection);

        MongoConnection.closeMongo();
    }





}
