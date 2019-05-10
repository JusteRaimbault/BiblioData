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
        Document res = new Document("id", reference.scholarID);
        res.append("title",reference.title.title);
        if(reference.authors.size()>0){res.append("author",reference.getAuthorString());}
        if(reference.resume.resume.length()>0){res.append("abstract",reference.resume.resume);}
        if(reference.resume.en_resume.length()>0){res.append("abstract_en",reference.resume.en_resume);}
        if(reference.keywords.size()>0){res.append("keywords",reference.getKeywordString());}
        if(reference.year.length()>0){res.append("year",reference.year);}
        if(reference.date.length()>0){res.append("date",reference.date);}
        res.append("citingFilled",reference.citingFilled);
        res.append("depth",reference.depth);
        res.append("origin",reference.origin);
        if(reference.horizontalDepth.keySet().size()>0){
            Document hdepth = new Document((Map) reference.horizontalDepth);
            res.append("horizontalDepth",hdepth);
        }
        return(res);
    }

    public static Reference fromDocument(Document document){
        String id = document.getString("id");
        String title = document.getString("title");// every doc should have title
        String year = document.getString("year");
        if(year==null){year="NA";}

        // add additional attributes by hand
        // FIXME data structure is messy and not secure - either systematize setters/getters, or go to scala ?
        // -> case class : immutable references / Links ? easy to combine with mongo cursors ?

        String horizDepth = "";
        if(document.containsKey("horizontalDepth")){
            for(String k :((Document) document.get("horizontalDepth")).keySet()){horizDepth=horizDepth+","+k+":"+((Document) document.get("horizontalDepth")).getInteger(k).toString();}
            //r.addAttribute("horizontalDepth",v.substring(1));
        }

        String depth = "";
        if(document.containsKey("depth")){
            depth = Integer.toString(document.getInteger("depth"));
            //r.addAttribute("depth",Integer.toString(document.getInteger("depth")));
        }

        String priority = "";
        if(document.containsKey("priority")){
            priority = Integer.toString(document.getInteger("priority"));
            //r.addAttribute("priority",Integer.toString(document.getInteger("priority")));
        }

        // construct attributes
        HashMap<String,String> attrs = new HashMap<>();
        attrs.put("horizontalDepth",horizDepth);
        attrs.put("depth",depth);
        attrs.put("priority",priority);

        // TODO add in Reference.construct : depth / horizdepth
        //Reference r = Reference.construct(id,title,year,attrs);
        Reference r = Reference.construct(id,title,year);

        return(r);
    }

    public static LinkedList<Document> citationLinksFromReference(Reference reference) {
        LinkedList<Document> links = new LinkedList<Document>();
        if(reference.citing.size()>0){
            for(Reference citing:reference.citing){
                Document link = new Document("from",citing.scholarID);
                link.append("to",reference.scholarID);
                links.add(link);
            }
        }
        if(reference.biblio.cited.size()>0){
            for(Reference cited: reference.biblio.cited){
                Document link = new Document("to",cited.scholarID);
                link.append("from",reference.scholarID);
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
