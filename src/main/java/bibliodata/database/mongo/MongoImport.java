
package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.OrderedCorpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.CSVReader;
import bibliodata.utils.Log;

import org.bson.Document;

import java.util.LinkedList;
import java.util.List;

public class MongoImport {


    /**
     * Import a corpus from file to mongo
     *
     * @param file csv corpus file
     * @param orderFile csv corpus giving request order for the first level
     * @param citedFolder
     * @param db
     * @param refcollection
     * @param citcollection
     * @param initDepth
     * @param origin
     * @param dropCollections
     */
    public static void fileToMongo(String file,
                                   String orderFile,
                                   String citationFile,
                                   String citedFolder,
                                   String db,
                                   String refcollection,
                                   String citcollection,
                                   int initDepth,
                                   String origin,
                                   boolean dropCollections){
        Corpus initial = Corpus.fromCSV(file,orderFile,citationFile,citedFolder,initDepth,origin);

        corpusToMongo(initial,db,refcollection,citcollection,dropCollections);

    }


    /**
     * Import citations only from a csv file
     * @param citationFile
     * @param db
     * @param citcollection
     */
    public static void citationsToMongo(String citationFile,String db,String citcollection,boolean dropCollection){
        String[][] rawlinks = CSVReader.read(citationFile, ";", "\"");
        Log.stdout("Links to import : "+rawlinks.length);

        MongoConnection.initMongo(db);

        if(dropCollection){MongoConnection.mongoDatabase.getCollection(citcollection).drop();}

        LinkedList<Document> links = new LinkedList<Document>();

        for (String[] link : rawlinks) {
            String from = link[0];
            String to = link[1];
            links.add(new Document("from",from).append("to",to));
        }

        MongoConnection.mongoInsert(citcollection,links);
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
