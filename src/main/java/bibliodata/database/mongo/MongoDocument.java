package bibliodata.database.mongo;

import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Reference;
import org.bson.Document;

import java.util.LinkedList;

public class MongoDocument {

    /**
     *
     * @param reference
     *
     * @requires reference has a non empty scholar id, on which mongo id is based ; non empty title
     *
     * @return
     */
    public static Document fromReference(Reference reference) {
        Document res = new Document("id", reference.scholarID);
        res.append("title",reference.title);
        if(reference.authors.size()>0){res.append("author",reference.getAuthorString());}
        if(reference.resume.resume.length()>0){res.append("abstract",reference.resume.resume);}
        if(reference.resume.en_resume.length()>0){res.append("abstract_en",reference.resume.en_resume);}
        if(reference.keywords.size()>0){res.append("keywords",reference.getKeywordString());}
        if(reference.year.length()>0){res.append("year",reference.year);}
        if(reference.date.length()>0){res.append("date",reference.date);}
        res.append("citingFilled",reference.citingFilled);
        return(res);
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