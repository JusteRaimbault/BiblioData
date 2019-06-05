
package bibliodata.database.mongo;

import bibliodata.Context;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.util.*;



/**
 * Handling class for the connection with a mongo database
 */
public class MongoConnection {

    /**
     * Global client
     */
    public static MongoClient mongoClient;

    /**
     * Global database
     */
    public static MongoDatabase mongoDatabase;


    /**
     *
     */
    public MongoCollection collection;

    /**
     * constructor assuming connection is initialized
     * @param collec
     */
    public MongoConnection(String collec){
        collection=mongoDatabase.getCollection(collec);
    }


    public static MongoCollection<Document> getCollection(String collectionName){return(mongoDatabase.getCollection(collectionName));}


    /**
     * Initialize the mongo client
     * @param host
     * @param port
     * @param db
     */
    public static void initMongo(String host, int port, String db) {
        try {
            mongoClient = new MongoClient( host , port );
            mongoDatabase = mongoClient.getDatabase(db);
            // set logip option only in the case of mongo
            Context.setLogips(true);
        } catch(Exception e){
            System.out.println("No mongo connection possible : ");
            e.printStackTrace();
        }
    }

    /**
     * new mongo with standard params
     * @param db
     */
    public static void initMongo(String db) {
        initMongo(Context.getMongoHost(),Context.getMongoPort(),db);
    }

    /**
     * switch the database
     *
     * @param newdb
     */
    public static void switchMongo(String newdb){
        closeMongo();
        initMongo(newdb);
    }


    /**
     * Close the mongo client
     */
    public static void closeMongo() {
        try{
            mongoClient.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }



}

