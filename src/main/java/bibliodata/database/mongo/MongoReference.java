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
     *
     * Rq: should merge docs with a merge strategy - but functional programming in java is more than a pain
     *  (see MongoRequest.upsert ad hoc merging procedure)
     *
     * @param r reference
     * @param collection collection
     * @param processing update for processing field
     */
    public static void updateReference(Reference r, String collection, boolean processing){
        MongoRequest.upsert(collection,"id",r.getId(),MongoDocument.fromReference(r).append("processing",processing));
    }




    /**
     * Get unfilled ref from db.
     * Criteria : filledRef = false ; depth > 0
     *
     * @param collection collection
     * @param maxPriority max priority for ref where it is defined (use depth for refs with no priority)
     * @return reference
     */
    public static Reference getUnfilled(String collection,int maxPriority) {
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        try {
            Document queryres;
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

            if (queryres!=null) {
                Reference res = Reference.construct(queryres.getString("id"), queryres.getString("title"));

                // depth is restricted to Int - problematic when using mongo interactively in parallel (numeric double by default)
                int depth;
                try {
                    depth = queryres.getInteger("depth");
                } catch (Exception e) {
                    throw new UnsupportedOperationException("Error while getting unfilled ref from mongo: depth is not an Integer - please update with NumberInt(.)");
                }
                res.setDepth(depth);

                return (res);
            }else{return(Reference.empty);}
        }catch(Exception e){
            Log.stdout("Error while getting unfilled ref:");
            e.printStackTrace();
            return(Reference.empty);
        }
    }

    public static void setProcessing(String id, String collection){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.findOneAndUpdate(eq("id",id),set("processing", true));
    }




    /**
     * get ref from mongo - one level of citing only to mimick sch collection - is it necessary ? ! mess around with levels ?
     * @param id id
     * @return reference
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
     * ref without getting citing
     * @param id id
     * @return reference
     */
    static Reference getRawReference(String id){
        return(MongoDocument.fromDocument(MongoRequest.findOne(Context.getReferencesCollection(),"id",id)));
    }


    /**
     * get raw set of all refs as mongo documents
     *
     *  depth can be negative: use effective min value to have all refs
     *
     * @param maxDepth maximal depth (strict)
     * @return HashMap (id -> Document)
     */
    static HashMap<String,Document> getAllRefsAsDocuments(int maxDepth,int numrefs){
        MongoCollection<Document> origrefscol = MongoConnection.getCollection(Context.getReferencesCollection());
        HashMap<String,Document> origrefs = new HashMap<>();
        Bson cond = gt("depth",maxDepth);
        for(Document d: origrefscol.find(cond)){
            if(origrefs.size()<numrefs||numrefs<0){origrefs.put(d.getString("id"),d);}
        }
        return(origrefs);
    }

    /**
     * Get refs as documents, given a maximal priority
     *  ! priority not always defined - exports refs with a priority only
     *
     * @param maxPriority max priority
     * @param maxDepth max depth
     * @param numrefs max number of refs
     * @return map (id -> Document)
     */
    static HashMap<String,Document>  getRefsPriorityAsDocuments(int maxPriority,int maxDepth, int numrefs){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(Context.getReferencesCollection());
        HashMap<String,Document> res = new HashMap<>();
        for(Document queryres :mongoCollection.find(
                and(
                        lt("priority",maxPriority),
                        gt("depth",maxDepth))
                )){
            if(res.size()<numrefs||numrefs<0){res.put(queryres.getString("id"),queryres);}
        }
        return(res);
    }



}
