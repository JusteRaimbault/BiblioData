package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.utils.CSVWriter;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.LinkedList;

import static bibliodata.database.mongo.MongoConnection.mongoClient;

public class MongoExport {


    /**
     * Export the current database (default parameters in Context) to a file
     * @param file
     */
    public static void export(int maxPriority,boolean withAbstracts,String file) {
        Corpus fullCorpus = MongoConnection.getCorpus(maxPriority);
        LinkedList<String> attributes = new LinkedList<>();attributes.add("depth");attributes.add("priority");attributes.add("horizontalDepth");
        fullCorpus.csvExport(file,withAbstracts,attributes);

        // FIXME add attributes export in csv export !!!
    }

    // not useful
    /*public static Corpus corpusFromMongo() {
        return(MongoConnection.getCorpus());
    }*/


}