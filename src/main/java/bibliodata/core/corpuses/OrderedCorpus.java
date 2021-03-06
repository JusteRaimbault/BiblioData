package bibliodata.core.corpuses;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import bibliodata.core.reference.Reference;
import bibliodata.utils.CSVWriter;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class OrderedCorpus extends Corpus {


    public LinkedList<Reference> orderedRefs;

    //public LinkedList<Reference> getOrderedRefs =

    /**
     * Empty corpus
     */
    public OrderedCorpus() {
        references = new HashSet<Reference>();
        orderedRefs = new LinkedList<Reference>();
    }

    public OrderedCorpus(Reference r) {
        references = new HashSet<Reference>();
        references.add(r);
        orderedRefs = new LinkedList<Reference>();
        orderedRefs.add(r);
    }


    /**
     * Corpus from existing set.
     *
     * @param refs
     */
    public OrderedCorpus(Set<Reference> refs) {
        references = new HashSet<Reference>(refs);
        orderedRefs = new LinkedList<Reference>();
        for(Reference r:references){
            orderedRefs.add(r);
        }
    }

    public OrderedCorpus(List<Reference> refs) {
        references = new HashSet<Reference>();
        orderedRefs = new LinkedList<Reference>();
        for(Reference r:refs){
            references.add(r);
            orderedRefs.add(r);
        }
    }

    public void addReference(Reference r){
        references.add(r);
        orderedRefs.add(r);
    }


    /**
     * Export to csv
     * Note : dirty as quite exactly same code -> would be easier with a trait mixin to define an order and not add an supplementary corpus class
     *
     * @param prefix
     * @param withAbstract
     */
    @Override
    public void csvExport(String prefix,boolean withAbstract){
        LinkedList<String[]> datanodes = new LinkedList<String[]>();
        LinkedList<String[]> dataedges = new LinkedList<String[]>();
        if(!withAbstract){String[] header = {"number","title","id","year"};datanodes.add(header);}else{String[] header = {"number","title","id","year","abstract","year"};datanodes.add(header);}
        int ind = 1;
        for(Reference r:orderedRefs){
            String[] row = {""};
            if(!withAbstract){String[] tmp = {ind+"",r.getTitle().title,r.getId(),r.getYear()};row=tmp;}
            else{
                String authorstr = "";for(String s:r.getAuthors()){authorstr=authorstr+s+",";}
                String[] tmp = {ind+"",r.getTitle().title,r.getId(),r.getYear(),r.getResume().resume,authorstr};
                row=tmp;
            }
            datanodes.add(row);
            for(Reference rc:r.getCiting()){String[] edge = {rc.getId(),r.getId()};dataedges.add(edge);}
          ind++;
        }
        CSVWriter.write(prefix+".csv", datanodes, ";", "\"");
        CSVWriter.write(prefix+"_links.csv", dataedges, ";", "\"");
    }



}