
package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;

import org.bson.Document;

import java.util.List;

public class MongoImport {


    /**
     * Import a corpus from file to mongo
     *
     * @param file
     * @param citedFolder
     * @param db
     * @param refcollection
     * @param citcollection
     * @param initDepth
     * @param origin
     * @param dropCollections
     */
    public static void fileToMongo(String file,String citedFolder,String db, String refcollection, String citcollection,int initDepth,String origin,boolean dropCollections){
        Corpus initial = Corpus.fromFile(file,citedFolder);
        Log.stdout("Imported corpus of size "+initial.references.size());
        //update the depth
        for(Reference r:initial){
            r.depth=initDepth;
            r.origin=origin;
        }

        corpusToMongo(initial,db,refcollection,citcollection,dropCollections);

    }

    /**
     * Import a corpus in mongo
     *
     * @param corpus
     * @param db
     * @param refcollection
     * @param citcollection
     * @param dropCollections
     */
    public static void corpusToMongo(Corpus corpus,String db,String refcollection,String citcollection,boolean dropCollections){
        MongoConnection.initMongo(db);

        Log.stdout("To import : "+corpus.references.size());

        if(dropCollections){
            MongoConnection.mongoDatabase.getCollection(refcollection).drop();
            MongoConnection.mongoDatabase.getCollection(citcollection).drop();
        }

        // better updating to avoid duplicates also in initial import
        MongoConnection.updateCorpus(corpus,refcollection,citcollection);

        MongoConnection.closeMongo();
    }





}
