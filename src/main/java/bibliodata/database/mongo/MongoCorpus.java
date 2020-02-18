package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;
import org.bson.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static bibliodata.database.mongo.MongoConnection.initMongo;

/**
 * Methods retrieving corpuses from mongo
 */
public class MongoCorpus {



    /**
     * Update a corpus of refs in the database
     * @param references
     */
    public static void updateCorpus(Corpus references, String refcollection, String linkcollection,boolean processing){
        for(Reference r:references){
            MongoReference.updateReference(r,refcollection,processing);
            MongoCitation.updateCitations(r,linkcollection);
        }
    }




    /**
     * Consolidate a corpus from several databases
     *
     * @param databases
     * @param maxPriority
     * @return
     *
     * TODO add dbprefix ? tricky the way corpus is constructed
     *
     */
    public static Corpus getConsolidatedCorpus(LinkedList<String> databases,int maxPriority,int maxDepth){
        if(databases.size()==0){return(new DefaultCorpus());}

        MongoConnection.initMongo(databases.get(0));
        for(String database: databases){
            MongoConnection.switchMongo(database);
            Corpus currentcorpus = getCorpus(maxPriority,maxDepth,-1); // always all refs for a consolidated corpus
            Log.stdout("For database "+database+" : "+currentcorpus.references.size()+" references ; total refs "+Reference.getNumberOfReferences());
        }

        return(new DefaultCorpus(Reference.getReferences()));
    }




    /**
     * reconstruct corpus for export
     *
     * @param maxPriority max priority to be exported
     * @requires mongo db is initialized
     * @return
     */
    public static Corpus getCorpus(int maxPriority, int maxDepth,int numrefs){
        HashMap<String,Document> refs = new HashMap<String,Document>();
        if(maxPriority==-1) {
            refs = MongoReference.getAllRefsAsDocuments(maxDepth,numrefs);}
        else {
            refs = MongoReference.getRefsPriorityAsDocuments(maxPriority,maxDepth,numrefs);
        }

        HashMap<String, Reference> corpus = new HashMap<String,Reference>();

        for(String id:refs.keySet()){
            Reference ref = MongoDocument.fromDocument(refs.get(id));
            corpus.put(id,ref);
        }

        // fill the citing sets by going through the links
        HashMap<String, LinkedList<String>> links = MongoCitation.getAllLinksMap();
        for(String to:links.keySet()){
            for(String from:links.get(to)) {
                // add links with two ends both in refs
                // -> filter links if maxPriority is provided
                if (refs.containsKey(to) && refs.containsKey(from)) {
                    Reference toRef = corpus.get(to);
                    Reference fromRef = corpus.get(from);
                    toRef.setCiting(fromRef);
                }
            }
        }
        return(new DefaultCorpus(new HashSet<Reference>(corpus.values())));
    }



}
