package bibliodata.database.mongo;


import bibliodata.Context;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.LinkedList;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;

/**
 * Methods retrieving references from mongo
 */
public class MongoReference {


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
        MongoRequest.upsert(collection,"id",r.getId(),MongoDocument.fromReference(r).append("processing",false));
    }




    /**
     * Get unfilled ref from db.
     * Criteria : filledRef = false ; depth > 0

     * @param collection
     * @return
     */
    public static Reference getUnfilled(String collection,int maxPriority) {
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        try {
            Document queryres = new Document();
            if (maxPriority>0) {
                // if priority is not set, maxPriority is taken as vertical depth
                queryres = mongoCollection.findOneAndUpdate(
                        and(
                                eq("citingFilled", false),
                                gt("depth", 0),
                                or(lt("priority", maxPriority),and(not(exists("priority")),gt("depth", maxPriority))),
                                or(not(exists("processing")), not(eq("processing", true)))
                        ),
                        set("processing", true)
                );
            } else {
                queryres = mongoCollection.findOneAndUpdate(
                        and(
                                eq("citingFilled", false),
                                gt("depth", 0),
                                or(not(exists("processing")), not(eq("processing", true)))
                        ),
                        set("processing", true)
                );
            }

            //Log.stdout(queryres.toJson());

            Reference res = Reference.construct(queryres.getString("id"), queryres.getString("title"));
            res.setDepth(queryres.getInteger("depth"));
            return (res);
        }catch(Exception e){return(null);}
    }




    /**
     * get ref from mongo - one level of citing only to mimick sch collection - is it necessary ? ! mess around with levels ?
     * @param id
     * @return
     */
    public static Reference getReference(String id){
        Reference existing = getRawReference(id);
        Log.stdout("from mongo : "+existing.getId());
        if(existing.isEmpty())
                //||(!existing.isCitingFilled())) // in some case may not be well set
          {return(existing);}
        else{
            // need to fill citing refs
            LinkedList<Reference> citing = MongoCitation.getCiting(existing.getId());
            if (citing.size()>0){
                // need to update depths
                for(Reference citingRef:citing){citingRef.setDepth(Math.max(existing.getDepth()-1,citingRef.getDepth()));citingRef.setCitingFilled(false);}
                existing.setCiting(citing);
                existing.setCitingFilled(true);

            }
            return(existing);
        }
    }

    /**
     * ref without getting cited
     * @param id
     * @return
     */
    public static Reference getRawReference(String id){
        return(MongoDocument.fromDocument(MongoRequest.findOne(Context.getReferencesCollection(),"id",id)));
    }


    /**
     * get raw set of all refs as mongo documents
     * @param maxDepth
     * @return HashMap is -> Document
     */
    public static HashMap<String,Document> getAllRefsAsDocuments(int maxDepth,int numrefs){
        MongoCollection<Document> origrefscol = MongoConnection.getCollection(Context.getReferencesCollection());
        HashMap<String,Document> origrefs = new HashMap<String,Document>();
        //  require that priority exists ? NO
        Bson cond = gt("depth",maxDepth);
        // FIXME not desirable behavior - depth can be negative indeed (why ?)
        //if(maxDepth==-1) cond = or(not(exists("depth")),gt("depth",maxDepth));
        for(Document d: origrefscol.find(
                cond
                //gt("depth",maxDepth) // depth may not be set for some ?
                //and(gt("depth",maxDepth),exists("priority")))
        )
        ){
            if(origrefs.size()<numrefs||numrefs<0){origrefs.put(d.getString("id"),d);}
        }
        return(origrefs);
    }

    /**
     * Get refs as documents, given a maximal priority
     * @param maxPriority
     * @param maxDepth
     * @return
     */
    public static HashMap<String,Document>  getRefsPriorityAsDocuments(int maxPriority,int maxDepth, int numrefs){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(Context.getReferencesCollection());
        HashMap<String,Document> res = new HashMap<String,Document>();
        // FIXME priority not always defined ?
        for(Document queryres :mongoCollection.find(
                and(
                        lt("priority",maxPriority), // do not use if no priority ?
                        gt("depth",maxDepth))
                )){
            if(res.size()<numrefs||numrefs<0){res.put(queryres.getString("id"),queryres);}
        }
        return(res);
    }



}
