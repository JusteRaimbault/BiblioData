package bibliodata.database.mongo;

import bibliodata.Context;
import bibliodata.core.corpuses.Corpus;
import bibliodata.utils.Log;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

/**
 * General commands on the mongo database
 */
public class MongoCommand {



    /**
     * increase all depths
     * @param collection
     */
    public static void incrAllDepths(String collection){
        //List<Document> docs = mongoFind(collection);
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.updateMany(gt("depth",-1),inc("depth",1));
    }

    public static void addTimeStamp(long ts,String collection){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.updateMany(gt("depth",-1),set("timestamp",ts));
    }

    /**
     * set processing to false
     * @param collection
     */
    public static void notProcessing(String collection){
        MongoCollection<Document> mongoCollection = MongoConnection.getCollection(collection);
        mongoCollection.updateMany(gt("depth",-1),set("processing",false));
    }


    /**
     * Add a priority field in a given db (collections by default, database assumed already init)
     *
     *  - for each max depth ref, get citing, compute min while constructing the graph
     */
    public static void computePriorities(int maxVerticalDepth){
        HashMap<String,Document> allrefs = getAllRefs(maxVerticalDepth);
        HashMap<String,LinkedList<String>> links = getAllLinks();

        LinkedList<Document> currentDepth = new LinkedList<Document>();
        for(Document d:allrefs.values()){if(d.getInteger("depth")==maxVerticalDepth){currentDepth.add(d);}}

        Log.stdout("Total refs : "+allrefs.size());
        Log.stdout("Total links : "+links.size());
        Log.stdout("Refs at max depth : "+currentDepth.size());

        HashMap<String,Integer> priorities = new HashMap<String,Integer>();

        for(int depth=maxVerticalDepth-1;depth>=0;depth=depth-1){

            LinkedList<Document> nextdepth = new LinkedList<Document>();

            for(Document d: currentDepth){
                LinkedList<Document> citing = getCitingAsDocuments(d,allrefs,links);
                int currentPriority = Context.getMaxHorizontalDepth();
                if(d.containsKey("horizontalDepth")){
                    Document h = (Document) d.get("horizontalDepth");
                    for(String k:h.keySet()){if(h.getInteger(k).intValue()<currentPriority){currentPriority=h.getInteger(k).intValue();}}
                }

                if (!d.containsKey("priority")){
                    if(priorities.containsKey(d.getString("id"))){if(currentPriority<priorities.get(d.getString("id")).intValue()){priorities.put(d.getString("id"),currentPriority);}}
                    else{priorities.put(d.getString("id"),currentPriority);}
                }

                for(Document c:citing){
                    nextdepth.add(c);// ! redundancy
                    if (!c.containsKey("priority")) {
                        if (priorities.containsKey(c.getString("id"))) {
                            if (currentPriority < priorities.get(c.getString("id")).intValue()) {
                                priorities.put(c.getString("id"), currentPriority);
                            }
                        } else {
                            priorities.put(c.getString("id"), currentPriority);
                        }
                    }
                }
            }

            // next level
            currentDepth = nextdepth;
        }
        // update in database
        Log.stdout("Priorities : updating "+priorities.size()+" references");
        for(String toupdate:priorities.keySet()){
            mongoUpdate("references","id",toupdate, "priority", priorities.get(toupdate).intValue());
        }
    }


    /**
     * consolidation into a single database
     * @param databases
     */
    public static void consolidate(LinkedList<String> databases){
        Corpus all = getConsolidatedCorpus(databases,-1,-1);

        // TODO should take into account citation updates here

        initMongo(Context.getCentralDatabase());
        updateCorpus(all, Context.getReferencesCollection(),Context.getCitationsCollection());
        closeMongo();
    }



    /**
     * filter on maxHorizDepth
     * @param originDB
     * @param targetDB
     * @param maxHorizDepth
     */
    // FIXME method not finished
    public static void filterDatabase(String originDB,String targetDB,int maxHorizDepth,int maxVerticalDepth) {
        initMongo(originDB);

        LinkedList<Document> newrefs = new LinkedList<Document>();

        HashMap<String,Document> origrefs = getAllRefs(maxVerticalDepth);

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
        HashMap<String,LinkedList<String>> links = MongoCitation.getAllLinksMap();

        LinkedList<Document> currentlevel = (LinkedList<Document>) newrefs.clone();
        for(int depth = 0;depth < maxVerticalDepth;depth++){

        }




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
