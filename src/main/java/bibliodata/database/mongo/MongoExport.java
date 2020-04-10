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
     * @param maxPriority maximal priority to export
     * @param maxDepth maximal depth
     * @param initLayerDepth depth of the origin layer (horizontal depth and filtering done from it)
     * @param numRefs number of references (-1: all references)
     * @param withAbstracts export abstracts if available
     * @param filter set of ids not to export
     * @param file file prefix for export (CSV for refs and links)
     */
    public static void export(int maxPriority, int maxDepth, int initLayerDepth, int numRefs, boolean withAbstracts, HashSet<String> filter,String file) {
        Log.stdout("Export maxPriority="+maxPriority+" ; maxDepth="+maxDepth+" ; initLayerDepth="+initLayerDepth+" ; numRefs="+numRefs);
        Corpus fullCorpus = MongoCorpus.getCorpus(maxPriority,maxDepth,numRefs);
        Log.stdout(fullCorpus.references.size()+" considered");
        // get hdepth names and propagate
        LinkedList<Reference> initlayer = new LinkedList<Reference>();
        for(Reference r:fullCorpus){
            if (r.getDepth()==initLayerDepth) {
                initlayer.add(r);
                if (filter.contains(r.getId())) { // set negative hdepth for refs to be filtered and propagate it
                    r.setHorizontalDepth("FILTERED",-1);
                }
            }
        }
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

        // filter using negative horizontal depths
        // FIXME does not remove refs from citing HashSets !
        Corpus toexport = new DefaultCorpus();
        for(Reference r:fullCorpus) {
            boolean toremove = false;
            for (String origin:r.getHorizontalDepthMap().keySet()){
                toremove=toremove||(r.getHorizontalDepth(origin)<0);
            }
            if (!toremove) toexport.references.add(r);
        }

        Log.stdout(toexport.references.size()+" refs to export");

        // set hdepth as attributes
        HashSet<String> attrs = new HashSet<String>();
        int hdepthcount = 0;
        for(Reference r:toexport){
            if(r.getHorizontalDepthMap().keySet().size()>0){hdepthcount++;}
            for(String origin:r.getHorizontalDepthMap().keySet()){
                attrs.add(origin);
                r.setAttribute(origin,r.getHorizontalDepthMap().get(origin).toString());
            }
        }
        Log.stdout("total hdepth count = "+hdepthcount);

        LinkedList<String> attributes = new LinkedList<>();attributes.add("depth");attributes.add("priority");attributes.add("horizontalDepth");attributes.add("citingFilled");

        // add horizontal depths
        attributes.addAll(attrs);

        toexport.csvExport(file,withAbstracts,attributes);
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