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
     * @param filterExclusive how to propagate filtered ids to lower layers: if filterExclusive, remove ref if contains filter, else remove ref if only contains filtered
     * @param keptHorizontalDepths set of horizontal depth label to keep (request level filter)
     * @param file file prefix for export (CSV for refs and links)
     */
    public static void export(int maxPriority,
                              int maxDepth,
                              int initLayerDepth,
                              int numRefs,
                              boolean withAbstracts,
                              HashSet<String> filter,
                              boolean filterExclusive,
                              HashSet<String> keptHorizontalDepths,
                              String file
    ) {
        Log.stdout("Export maxPriority="+maxPriority+" ; maxDepth="+maxDepth+" ; initLayerDepth="+initLayerDepth+" ; numRefs="+numRefs);
        Corpus fullCorpus = MongoCorpus.getCorpus(maxPriority,maxDepth,numRefs);
        Log.stdout(fullCorpus.references.size()+" considered");
        // get hdepth names and propagate
        LinkedList<Reference> initlayer = new LinkedList<>();
        for(Reference r:fullCorpus){
            if (r.getDepth()==initLayerDepth) {
                initlayer.add(r);
                boolean filtered = false;
                for(String f : filter){filtered=filtered||r.getId().startsWith(f);}
                if (filtered) {
                    // set negative hdepth for refs to be filtered and propagate it
                    Log.stdout("    Filtered: "+r.toString());
                    // to ensure removing manually filtered at first layer in any case, remove all other hdepths than filtered
                    r.resetHorizontalDepth();
                    r.setHorizontalDepth("FILTERED",-1);
                }
            }
        }

        if (filter.size()>0)
        //for(Reference r:fullCorpus) {}

        Log.stdout("init layer size = "+initlayer.size());
        int n = initlayer.size();int i =0;
        for(Reference r0:initlayer){
            //Log.stdout("setting hdepth : "+i+" / "+n);
            if(i%50==0){Log.stdout(" "+i+" / "+n+" ",false);}else{Log.stdout("=",false);}
            HashMap<String,Integer> origs = r0.getHorizontalDepthMap();
            //Log.stdout(" in-cit links = "+r0.getCiting().size());
            for(String origin:origs.keySet()){
                //Log.stdout(origin+":"+origs.get(origin));
                r0.setHorizontalDepth0(origin,origs.get(origin));
            }
            i++;
        }

        // DEBUG : at this step ALL refs in the corpus should have some hdepth

        // filter using negative and kept horizontal depths
        Corpus toexport = new DefaultCorpus();
        Corpus toremove = new DefaultCorpus();

        for(Reference r:fullCorpus) {
            boolean removeNeg =  !filterExclusive;
            boolean removeKept = true;
            for (String origin:r.getHorizontalDepthMap().keySet()){
                if(filterExclusive) removeNeg=removeNeg||(r.getHorizontalDepth(origin)<0); else removeNeg=removeNeg&&(r.getHorizontalDepth(origin)<0);
                removeKept = removeKept&&!keptHorizontalDepths.contains(origin);
            }
            boolean remove = removeNeg&&removeKept;
            if (!remove) toexport.references.add(r);
            else {
                Log.stdout("Removing: "+r.getId());
                toremove.references.add(r);
            }
        }

        // second pass to remove refs from citing HashSets in the exported corpus
        for(Reference r:toexport) {
            HashSet<Reference> citing = new HashSet<>(r.getCiting());
            for(Reference c:citing){
                if (toremove.references.contains(c)) r.getCiting().remove(c);
            }
        }

        Log.stdout(toremove.references.size()+" refs to remove");
        Log.stdout(toexport.references.size()+" refs to export");

        // set hdepth as attributes
        HashSet<String> attrs = new HashSet<>();
        int hdepthcount = 0;
        for(Reference r:toexport){
            if(r.getHorizontalDepthMap().keySet().size()>0){hdepthcount++;}
            for(String origin:r.getHorizontalDepthMap().keySet()){
                attrs.add(origin);
                r.setAttribute(origin,r.getHorizontalDepthMap().get(origin).toString());
            }
        }
        Log.stdout("total hdepth count = "+hdepthcount);

        LinkedList<String> attributes = new LinkedList<>();
        attributes.add("depth");
        //attributes.add("priority");
        //attributes.add("horizontalDepth");
        attributes.add("citingFilled");

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