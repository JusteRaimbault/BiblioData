package bibliodata.database.mongo;


import bibliodata.Context;
import bibliodata.core.reference.Reference;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import bibliodata.utils.Log;

/**
 * Methods retrieving citation links from mongo
 */
public class MongoCitation {



    /**
     *
     * @param r reference
     * @param linkcol link collection
     */
    public static void updateCitations(Reference r, String linkcol){
        List<Document> links = MongoDocument.citationLinksFromReference(r);
        LinkedList<String> linkids = new LinkedList<>();linkids.add("from");linkids.add("to");
        MongoRequest.upsert(linkcol,links,linkids);
    }



    /**
     * get citing docs for a list of cited docs
     * @param cited cited docs
     * @param alldocs hashmap ID -> doc with all documents
     * @param alllinks hashmap ID -> citing IDs
     * @return
     */
    private static LinkedList<Document> getCitingAsDocuments(LinkedList<Document> cited, HashMap<String,Document> alldocs, HashMap<String,LinkedList<String>> alllinks){
        LinkedList<Document> res = new LinkedList<Document>();
        for(Document d:cited){
            LinkedList<String> links = alllinks.get(d.getString("id"));
            if(links!=null) {// may be not cited
                for (String citing : links) {
                    if (alldocs.containsKey(citing)) {
                        res.add(alldocs.get(citing));
                    }
                }
            }
        }
        return(res);
    }

    /**
     * get citing for a doc given all docs and all links
     * @param cited cited doc
     * @param alldocs all documents
     * @param alllinks all links
     * @return
     */
    public static LinkedList<Document> getCitingAsDocuments(Document cited,HashMap<String,Document> alldocs,HashMap<String,LinkedList<String>> alllinks){
        LinkedList<Document> docs = new LinkedList<Document>();
        docs.add(cited);
        return(getCitingAsDocuments(docs,alldocs,alllinks));
    }



    /**
     * get citing References given cited id
     * @param citedId
     * @return
     */
    public static LinkedList<Reference> getCiting(String citedId){
        LinkedList<Reference> res = new LinkedList<>();
        Log.stdout("links for "+citedId);
	LinkedList<Document> links = MongoRequest.find(Context.getCitationsCollection(),"to",citedId);
        for(Document link:links){
            // \!/ DANGER - getReference / getRawReference is not optimal, risk of self recursive infinite call
            //Log.stdout("link from mongo : "+link.getString("from")+"-"+link.getString("to"));
	    res.add(MongoReference.getRawReference(link.getString("from")));//link has necessarily 'from' record
        }
        return(res);
    }


    /**
     * get all links as hashmap (default cit col)
     * @return
     */
    public static HashMap<String,LinkedList<String>> getAllLinksMap(){
        MongoCollection<Document> origlinks = MongoConnection.getCollection(Context.getCitationsCollection());
        HashMap<String,LinkedList<String>> links = new HashMap<String,LinkedList<String>>();
        for(Document l:origlinks.find()){
            if(links.containsKey(l.getString("to"))){
                links.get(l.getString("to")).add(l.getString("from"));
            }
            else{
                LinkedList<String> citing = new LinkedList<String>();citing.add(l.getString("from"));
                links.put(l.getString("to"),citing);}
        }
        return(links);
    }

    /**
     * links as to,from
     * @return
     */
    public static HashMap<String,String> getAllLinksRawMap(boolean from){
        MongoCollection<Document> origlinks = MongoConnection.getCollection(Context.getCitationsCollection());
        HashMap<String,String> links = new HashMap<String,String>();
        for(Document l:origlinks.find()){
            if(l.containsKey("to")&&l.containsKey("from")){
                if(from==true) {
                    links.put(l.getString("from"),l.getString("to"));
                }else{
                    links.put(l.getString("to"),l.getString("from"));
                }
            }
        }
        return(links);
    }

}
