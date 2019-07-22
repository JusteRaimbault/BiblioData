package bibliodata.database.mongo;

import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Reference;
import bibliodata.utils.Log;
import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MongoDocument {



    /**
     *
     * @param reference
     *
     * @requires reference has a non empty scholar id, on which mongo id is based ; non empty title
     *
     * @return
     */
    public static Document fromReference(Reference reference){
        //Log.stdout(reference.toString());
        Document res = new Document("id", reference.getId());
        res.append("title",reference.getTitle().title);
        if(reference.getAuthors().size()>0){res.append("author",reference.getAuthorString());}
        if(reference.getResume().resume.length()>0){res.append("abstract",reference.getResume().resume);}
        if(reference.getResume().en_resume.length()>0){res.append("abstract_en",reference.getResume().en_resume);}
        if(reference.getKeywords().size()>0){res.append("keywords",reference.getKeywordString());}
        if(reference.getYear().length()>0){res.append("year",reference.getYear());}
        //if(reference.date.length()>0){res.append("date",reference.date);}
        res.append("citingFilled",reference.isCitingFilled());
        res.append("depth",reference.getDepth());
        res.append("origin",reference.getOrigin());
        if(reference.getHorizontalDepthMap().keySet().size()>0){
            Document hdepth = new Document((Map) reference.getHorizontalDepthMap());
            res.append("horizontalDepth",hdepth);
        }
        return(res);
    }

    /**
     * mongo document to Reference
     * @param document
     * @return
     */
    public static Reference fromDocument(Document document){
        if(document.keySet().size()==0){return(Reference.empty);}//better avoid returning null
        else {
            String id = document.getString("id");

            String title = document.getString("title");// every doc should have title
            String year = document.getString("year");
            if (year == null) {
                year = "NA";
            }

            // add additional attributes by hand
            // FIXME data structure is messy and not secure - either systematize setters/getters, or go to scala ?
            // -> case class : immutable references / Links ? easy to combine with mongo cursors ?

            String horizDepth = "";
            if (document.containsKey("horizontalDepth")) {
                for (String k : ((Document) document.get("horizontalDepth")).keySet()) {
                    horizDepth = horizDepth + "," + k + ":" + ((Document) document.get("horizontalDepth")).getInteger(k).toString();
                }
                //r.addAttribute("horizontalDepth",v.substring(1));
                if (horizDepth.length() > 0) {
                    horizDepth = horizDepth.substring(1);
                }// remove first comma
            }

            String depth = "";
            if (document.containsKey("depth")) {
                depth = Integer.toString(document.getInteger("depth"));
                //r.addAttribute("depth",Integer.toString(document.getInteger("depth")));
            }

            String priority = "";
            if (document.containsKey("priority") && document.get("priority").toString().length() > 0) {
                priority = Integer.toString(document.getInteger("priority"));
                //r.addAttribute("priority",Integer.toString(document.getInteger("priority")));
            }

            // construct attributes
            HashMap<String, String> attrs = new HashMap<>();
            attrs.put("horizontalDepth", horizDepth);
            attrs.put("depth", depth);
            attrs.put("priority", priority);

            Reference r = Reference.construct(id, title, year, attrs);

            // check if citingFilled and add if exists (case for consolidated refs)
            if (document.containsKey("citingFilled")) {
                r.setCitingFilled(document.getBoolean("citingFilled"));
            }
            // redundant properties with generic attributes // FIXME shitty architecture (again :/ )
            if (document.containsKey("depth")) {
                r.setDepth(Integer.parseInt(depth));
            }
            if (document.containsKey("horizontalDepth")) {
                for (String k : ((Document) document.get("horizontalDepth")).keySet()) {
                    r.setHorizontalDepth(k,((Document) document.get("horizontalDepth")).getInteger(k));
                }
            }


            return (r);
        }
    }

    public static LinkedList<Document> citationLinksFromReference(Reference reference) {
        LinkedList<Document> links = new LinkedList<Document>();
        if(reference.getCiting().size()>0){
            for(Reference citing:reference.getCiting()){
                Document link = new Document("from",citing.getId());
                link.append("to",reference.getId());
                links.add(link);
            }
        }
        if(reference.getCited().size()>0){
            for(Reference cited: reference.getCited()){
                Document link = new Document("to",cited.getId());
                link.append("from",reference.getId());
                links.add(link);
            }
        }
        return(links);
    }

    public static LinkedList<Document> fromCorpus(Corpus corpus){
        LinkedList<Document> docs = new LinkedList();
        for(Reference r:corpus){
            docs.add(fromReference(r));
        }
        return(docs);
    }

    public static LinkedList<Document> citationLinksFromCorpus(Corpus corpus){
        LinkedList<Document> links = new LinkedList();
        for(Reference r:corpus){
            links.addAll(citationLinksFromReference(r));
        }
        return(links);
    }


}
