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
    public static void export(int maxPriority,int maxDepth,boolean withAbstracts,String file) {
        Corpus fullCorpus = MongoConnection.getCorpus(maxPriority,maxDepth);
        LinkedList<String> attributes = new LinkedList<>();attributes.add("depth");attributes.add("priority");attributes.add("horizontalDepth");attributes.add("citingFilled");
        fullCorpus.csvExport(file,withAbstracts,attributes);
    }

    /**
     * export consolidated corpus from different databases
     * @param maxPriority
     * @param databases
     * @param file
     */
    public static void exportConsolidated(int maxPriority,int maxDepth,LinkedList<String> databases,String file) {
        Corpus consolidated = MongoConnection.getConsolidatedCorpus(databases,maxPriority,maxDepth);
        LinkedList<String> attributes = new LinkedList<>();attributes.add("depth");attributes.add("priority");attributes.add("horizontalDepth");attributes.add("citingFilled");
        consolidated.csvExport(file,false,attributes);
    }




}