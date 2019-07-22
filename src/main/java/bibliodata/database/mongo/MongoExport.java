package bibliodata.database.mongo;


import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.CSVWriter;
import bibliodata.utils.Log;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static bibliodata.database.mongo.MongoConnection.mongoClient;

public class MongoExport {


    /**
     * Export the current database (default parameters in Context) to a file
     * @param file
     */
    public static void export(int maxPriority,int maxDepth,int initLayerDepth,int numRefs,boolean withAbstracts,String file) {
        Log.stdout("Export maxPriority="+maxPriority+" ; maxDepth="+maxDepth+" ; initLayerDepth="+initLayerDepth+" ; numRefs="+numRefs);
        Corpus fullCorpus = MongoCorpus.getCorpus(maxPriority,maxDepth,numRefs);
        Log.stdout(fullCorpus.references.size()+" refs to export");
        // get hdepth names and propagate
        LinkedList<Reference> initlayer = new LinkedList<Reference>();
        for(Reference r:fullCorpus){if (r.getDepth()==initLayerDepth)initlayer.add(r);}
        Log.stdout("init layer size = "+initlayer.size());
        int n = initlayer.size();int i =0;
        for(Reference r0:initlayer){
            Log.stdout("setting hdepth : "+i+" / "+n);
            HashMap<String,Integer> origs = r0.getHorizontalDepthMap();
            Log.stdout(" in-cit links = "+r0.getCiting().size());
            for(String origin:origs.keySet()){
                Log.stdout(origin+":"+origs.get(origin));
                r0.setHorizontalDepth0(origin,origs.get(origin));
            }
            i++;
        }

        // DEBUG : at this step ALL refs in the corpus should have some hdepth

        // set hdepth as attributes
        HashSet<String> attrs = new HashSet<String>();
        int hdepthcount = 0;
        for(Reference r:fullCorpus){
            if(r.getHorizontalDepthMap().keySet().size()>0){hdepthcount++;}
            for(String origin:r.getHorizontalDepthMap().keySet()){
                attrs.add(origin);
                r.setAttribute(origin,r.getHorizontalDepthMap().get(origin).toString());
            }
        }
        Log.stdout("total hdepth count = "+hdepthcount);

        LinkedList<String> attributes = new LinkedList<>();attributes.add("depth");attributes.add("priority");attributes.add("horizontalDepth");attributes.add("citingFilled");

        // add horizontal depths
        for(String hattr:attrs){attributes.add(hattr);}

        fullCorpus.csvExport(file,withAbstracts,attributes);
    }

    /**
     * export consolidated corpus from different databases
     * @param maxPriority
     * @param databases
     * @param file
     */
    public static void exportConsolidated(int maxPriority,int maxDepth,LinkedList<String> databases,String file) {
        Corpus consolidated = MongoCorpus.getConsolidatedCorpus(databases,maxPriority,maxDepth);
        LinkedList<String> attributes = new LinkedList<>();attributes.add("depth");attributes.add("priority");attributes.add("horizontalDepth");attributes.add("citingFilled");
        consolidated.csvExport(file,false,attributes);
    }




}