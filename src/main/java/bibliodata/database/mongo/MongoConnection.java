
package bibliodata.database.mongo;

import bibliodata.Context;
import bibliodata.core.corpuses.Corpus;
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

    public static void initMongo(String db) {
        initMongo("127.0.0.1",27017,db);
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


    /**
     * Update a corpus of refs in the database
     * @param references
     */
    public static void updateCorpus(Corpus references, String refcollection, String linkcollection){
        for(Reference r:references){
            updateReference(r,refcollection);
            updateCitations(r,linkcollection);
        }
    }

    /**
     * Update a single reference
     * @param r
     * @param collection
     */
    public static void updateReference(Reference r, String collection){
        // this must be done in the construction/update
        //r.citingFilled = citationsCollected;
        //Document prev = mongoFindOne(collection,"id",r.scholarID);
        //if(prev.containsKey("id")){}
        //else{mongoInsert(collection,MongoDocument.fromReference(r));}

        // FIXME should merge docs with a merge strategy - but functional programming in java is more than a pain
        // do not insert if already at a higher depth
        mongoUpsert(collection,"id",r.scholarID,MongoDocument.fromReference(r).append("processing",false));
    }

    /**
     *
     * @param r
     * @param linkcol
     */
    public static void updateCitations(Reference r,String linkcol){
        List<Document> links = MongoDocument.citationLinksFromReference(r);
        // insert only if not already
        //mongoInsert(linkcol,links);
        mongoUpsert(linkcol,links);
    }

    /**
     * find one with key,val
     * @param collection
     * @param key
     * @param value
     * @return
     */
    public static Document mongoFindOne(String collection,String key,Object value){
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        FindIterable res = mongoCollection.find(eq(key,value));
        if(res.iterator().hasNext()){return((Document) res.iterator().next());}
        else{return new Document();}
    }

    /**
     * find all in a collection
     * @param collection
     * @return
     */
    public static List<Document> mongoFind(String collection){
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        FindIterable resquery = mongoCollection.find();
        List<Document> res = new LinkedList<Document>();
        while(resquery.iterator().hasNext()){
            res.add((Document) resquery.iterator().next());
        }
        return(res);
    }

    /**
     * Document insertion
     * @param collection
     * @param document
     */
    public static void mongoInsert(String collection, Document document) {
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.insertOne(document);
    }

    public void mongoInsert(Document document){collection.insertOne(document);}

    /**
     * List of documents insertion
     * @param collection
     * @param documents
     */
    public static void mongoInsert(String collection, List<Document> documents) {
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        if(documents.size()>0){mongoCollection.insertMany(documents);}
    }

    public void mongoInsert(List<Document> documents){collection.insertMany(documents);}

    /**
     * insert document if do not exist
     *
     * // FIXME the field-specific update strategy should be an argument - however difficult without func prog
     *   - case class MergeStrategy(
     *        existingFieldsStrat: Map[field: String -> (condition,operator)],
     *        addFieldsStrat: (condition,operator)
     *        )
     *        -> terrible, keep quickndirty approach for now
     *
     * @param collection
     * @param idkey
     * @param idvalue
     * @param document
     */
    public static void mongoUpsert(String collection, String idkey,Object idvalue, Document document){
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        //  doing something when already present is distinguished (fields to be updated)
        Document existing = mongoFindOne(collection,idkey,idvalue);
        //Log.stdout("Upsert on existing : "+existing.toJson());
        if(existing.keySet().size()>0){
            List<Bson> updates = new LinkedList<Bson>();

            for(String k:document.keySet()){
                // update all non specific keys
                if(!k.equals("_id")&
                   !k.equals("depth")&
                   !k.equals("id")&
                   !k.equals("citingFilled")&
                   !k.equals("origin")&
                   !k.equals("horizontalDepth")
                ){
                      updates.add(set(k,document.get(k)));
                }
            }

            if(document.containsKey("depth")) {
                // update depth only if older depth is smaller
                if(existing.containsKey("depth")) {
                    if (existing.getInteger("depth") < document.getInteger("depth")) {
                        updates.add(set("depth", document.getInteger("depth")));
                    }
                }else{
                    updates.add(set("depth", document.getInteger("depth")));
                }
            }

            if(document.containsKey("citingFilled")) {
                // update citingFilled with a or
                if(existing.containsKey("citingFilled")) {
                    updates.add(set("citingFilled", document.getBoolean("citingFilled") || existing.getBoolean("citingFilled")));
                }else{
                    updates.add(set("citingFilled", document.getBoolean("citingFilled")));
                }
            }


            if(document.containsKey("origin")) {
                // concatenate origins if the document has the origin key
                if (existing.containsKey("origin")) {
                    if (existing.getString("origin").length() > 0 & !existing.getString("origin").contains(document.getString("origin"))) {
                        updates.add(set("origin", existing.getString("origin") + ";" + document.getString("origin")));
                    } else {
                        updates.add(set("origin", document.getString("origin")));
                    }
                }else {
                    updates.add(set("origin", document.getString("origin")));
                }
            }

            if(document.containsKey("horizontalDepth")){
                if(existing.containsKey("horizontalDepth")){
                    // merge the maps - in case of key conflict : duplicate
                    // new doc as cannot copy the generic object
                    // Lenses would be cool here
                    Document merged = new Document();
                    for(String k1 : ((Document)document.get("horizontalDepth")).keySet()){merged.append(k1,((Document)document.get("horizontalDepth")).get(k1));}
                    for(String k2 : ((Document) existing.get("horizontalDepth")).keySet()){
                        /*if(merged.containsKey(k2)){merged.append(k2+"_1",((Document) existing.get("horizontalDepth")).get(k2));}
                        else {merged.append(k2,((Document) existing.get("horizontalDepth")).get(k2));}*/
                        merged.append(k2,((Document) existing.get("horizontalDepth")).get(k2));
                    }
                    updates.add(set("horizontalDepth",merged));
                }else{
                    updates.add(set("horizontalDepth", document.getString("horizontalDepth")));
                }
            }

            // apply updates
            mongoCollection.updateOne(eq(idkey, idvalue), combine(updates),(new UpdateOptions()).upsert(true));
        }else{
            mongoInsert(collection,document);
        }
    }



    /**
     * insert documents if not exactly the same already
     * @param collection
     * @param documents
     */
    public static void mongoUpsert(String collection,List<Document> documents){
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        for(Document document:documents){
            //mongoCollection.updateOne(document,document,(new UpdateOptions()).upsert(true));
            List<Bson> filter = new LinkedList<Bson>();
            for(String k:document.keySet()){if(!k.equals("_id")){filter.add(eq(k,document.get(k)));}}
            mongoCollection.replaceOne(and(filter),document,(new UpdateOptions()).upsert(true));
        }
    }


    /**
     * update one field with one id field
     * @param collection
     * @param idkey
     * @param idvalue
     * @param updatekey
     * @param updatevalue
     */
    public static void mongoUpdate(String collection, String idkey, Object idvalue, String updatekey, Object updatevalue) {
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.updateOne(eq(idkey, idvalue), set(updatekey, updatevalue));
    }

    /**
     * update several fields with several ids
     * @param collection
     * @param ids
     * @param update
     */
    public static void mongoUpdate(String collection, Map<String,Object> ids, Map<String,Object> update){
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        List<Bson> filters = new LinkedList<Bson>();
        for(String k:ids.keySet()){filters.add(eq(k,ids.get(k)));}
        List<Bson> updates = new LinkedList<Bson>();
        for(String k:update.keySet()){updates.add(set(k,update.get(k)));}
        mongoCollection.updateOne(and(filters),combine(updates));
    }


    public static void mongoDelete(String collection, String idkey, Object idvalue) {
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.deleteOne(eq(idkey, idvalue));
    }



    /**
     * Get unfilled ref from db.
     * Criteria : filledRef = false ; depth > 0

     * @param collection
     * @return
     */
    public static Reference getUnfilled(String collection) {
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        try {
            Document queryres = mongoCollection.findOneAndUpdate(
                    and(
                        eq("citingFilled", false),
                        gt("depth",0),
                        or(not(exists("processing")),not(eq("processing",true)))
                    ),
                    set("processing",true)
            );
            Log.stdout(queryres.toJson());
            Reference res = Reference.construct(queryres.getString("id"), queryres.getString("title"));
            res.depth = queryres.getInteger("depth");
            return (res);
        }catch(Exception e){return(null);}
    }


    /**
     * increase all depths
     * @param collection
     */
    public static void incrAllDepths(String collection){
        //List<Document> docs = mongoFind(collection);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.updateMany(gt("depth",-1),inc("depth",1));
    }

    /**
     * set processing to false
     * @param collection
     */
    public static void notProcessing(String collection){
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        mongoCollection.updateMany(gt("depth",-1),set("processing",false));
    }


    /**
     * filter on maxHorizDepth
     * @param originDB
     * @param targetDB
     * @param maxHorizDepth
     */
    public static void filterDatabase(String originDB,String targetDB,int maxHorizDepth,int maxVerticalDepth) {
        initMongo(originDB);
        MongoCollection<Document> origrefscol = mongoDatabase.getCollection(Context.getReferencesCollection());
        MongoCollection<Document> origlinks = mongoDatabase.getCollection(Context.getCitationsCollection());

        LinkedList<Document> newrefs = new LinkedList<Document>();
        HashMap<String,Document> origrefs = new HashMap<String,Document>();
        for(Document d: origrefscol.find()){origrefs.put(d.getString("id"),d);}

        for(Document d:origrefs.values()){
            // filter by hand : first level = max depth, maxHorizDepth
            if(d.getInteger("depth").intValue()==maxVerticalDepth){
                if(d.containsKey("horizontalDepth")){
                    Document h = (Document) d.get("horizontalDepth");
                    boolean toadd = false;
                    for(String k:h.keySet()){toadd = toadd||(h.getInteger(k)<=maxHorizDepth);}
                    if(toadd==true){newrefs.add(d);}
                }
            }
        }

        // use links to reconstruct nw
        HashMap<String,LinkedList<String>> links = new HashMap<String,LinkedList<String>>();
        for(Document l:origlinks.find()){
            if(links.containsKey(l.getString("to"))){
                links.get(l.getString("to")).add(l.getString("from"));
            }
            else{
                LinkedList<String> citing = new LinkedList<String>();citing.add(l.getString("from"));
                links.put(l.getString("to"),citing);}
        }

        LinkedList<Document> currentlevel = (LinkedList<Document>) newrefs.clone();
        for(int depth = 0;depth < maxVerticalDepth;depth++){

        }




    }

    private static LinkedList<Document> getCiting(LinkedList<Document> cited,HashMap<String,Document> alldocs,HashMap<String,LinkedList<String>> alllinks){
        LinkedList<Document> res = new LinkedList<Document>();
        for(Document d:cited){
            LinkedList<String> links = alllinks.get(d.getString("id"));
            for(String citing:links){
                if(alldocs.containsKey(citing)){res.add(alldocs.get(citing));}
            }
        }
        return(res);
    }


    /**
     * log ip
     *
     *  number of collected references : record with ts = -1 ?
     *
     * @param ip
     * @param success
     */
    public static void logIP(String ip,boolean success,int collectedRefs) {
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(Context.getNetstatCollection());
        String timestamp = new Long((new Date()).getTime()).toString();

        Document toinsert = new Document("ip",ip).append("success",success).append("ts",timestamp);
        mongoCollection.insertOne(toinsert);

        Document existingcount = mongoFindOne(Context.getNetstatCollection(),"ipcount",ip);
        if(existingcount.keySet().size()>0){
            mongoCollection.updateOne(eq("ipcount",ip),inc("docs",collectedRefs));
        }else{
            Document newcount = new Document("ipcount",ip).append("docs",collectedRefs);
            mongoCollection.insertOne(newcount);
        }


    }







}

