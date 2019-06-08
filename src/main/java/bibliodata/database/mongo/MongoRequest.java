package bibliodata.database.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;


/**
 * Generic requests on the mongo database
 */
public class MongoRequest {


    /**
     * find one with key,val
     * @param collection
     * @param key
     * @param value
     * @return
     */
    public static Document findOne(String collection, String key, Object value){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        FindIterable res = mongoCollection.find(eq(key,value));
        if(res.iterator().hasNext()){return((Document) res.iterator().next());}
        else{return new Document();}
    }

    /**
     * find all in a collection
     * @param collection
     * @return
     */
    public static List<Document> find(String collection){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        FindIterable resquery = mongoCollection.find();
        List<Document> res = new LinkedList<Document>();
        MongoCursor<Document> cursor = resquery.iterator();
	try {
           while(cursor.hasNext()){
		   Document nextdoc = cursor.next();
		   //System.out.println(nextdoc);
            res.add(nextdoc);
           }
	}finally {
           cursor.close();
        }
	return(res);
    }

    /**
     * find all given a request
     * @param collection
     * @param key
     * @param value
     * @return
     */
    public static LinkedList<Document> find(String collection, String key, Object value){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        FindIterable res = mongoCollection.find(eq(key,value));
        LinkedList<Document> found = new LinkedList<>();
        MongoCursor<Document> cursor = res.iterator();
        try {
           while(cursor.hasNext()){
                   Document nextdoc = cursor.next();
                   //System.out.println(nextdoc);
            found.add(nextdoc);
           }
        }finally {
           cursor.close();
        }
	return(found);
    }

    /**
     * Document insertion
     * @param collection
     * @param document
     */
    public static void insert(String collection, Document document) {
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.insertOne(document);
    }

    //public void mongoInsert(Document document){collection.insertOne(document);}

    /**
     * List of documents insertion
     * @param collection
     * @param documents
     */
    public static void insert(String collection, List<Document> documents) {
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        if(documents.size()>0){mongoCollection.insertMany(documents);}
    }

    // FIXME do not instantiate objects of this class ? (should be a scala object)
    //public void mongoInsert(List<Document> documents){collection.insertMany(documents);}

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
    public static void upsert(String collection, String idkey,Object idvalue, Document document){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);

        //  doing something when already present is distinguished (fields to be updated)
        Document existing = findOne(collection,idkey,idvalue);
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
            insert(collection,document);
        }
    }



    /**
     * insert documents if not exactly the same already
     * @param collection
     * @param documents
     */
    public static void upsert(String collection,List<Document> documents){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
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
    public static void update(String collection, String idkey, Object idvalue, String updatekey, Object updatevalue) {
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.updateOne(eq(idkey, idvalue), set(updatekey, updatevalue));
    }

    /**
     * update several fields with several ids
     * @param collection
     * @param ids
     * @param update
     */
    public static void update(String collection, Map<String,Object> ids, Map<String,Object> update){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        List<Bson> filters = new LinkedList<Bson>();
        for(String k:ids.keySet()){filters.add(eq(k,ids.get(k)));}
        List<Bson> updates = new LinkedList<Bson>();
        for(String k:update.keySet()){updates.add(set(k,update.get(k)));}
        mongoCollection.updateOne(and(filters),combine(updates));
    }


    public static void delete(String collection, String idkey, Object idvalue) {
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.deleteOne(eq(idkey, idvalue));
    }



}
