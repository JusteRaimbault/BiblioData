package bibliodata.utils;

import bibliodata.core.reference.Abstract;
import bibliodata.core.reference.Reference;
import bibliodata.core.reference.Title;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.LaTeXObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BIBReader {

    /**
     * Read a bibtex file using jbibtex
     * @param filePath
     * @return
     */
    public static HashSet<Reference> read(String filePath) {
        HashSet<Reference> refs = new HashSet<Reference>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
            org.jbibtex.BibTeXParser bibtexParser = new org.jbibtex.BibTeXParser();
            org.jbibtex.LaTeXPrinter latexPrinter = new org.jbibtex.LaTeXPrinter();
            org.jbibtex.LaTeXParser latexParser = new org.jbibtex.LaTeXParser();
            BibTeXDatabase database = bibtexParser.parse(reader);
            Map<Key,BibTeXEntry> entries = database.getEntries();
            for(Key key :entries.keySet()){
                BibTeXEntry entry = entries.get(key);
                //System.out.println((entry.getField(BibTeXEntry.KEY_TITLE)).toUserString());
                try {
                    String t = latexPrinter.print(latexParser.parse((entry.getField(BibTeXEntry.KEY_TITLE)).toUserString()));
                    String y = latexPrinter.print(latexParser.parse((entry.getField(BibTeXEntry.KEY_YEAR)).toUserString()));
                    // FIXME other fields should be imported
                    refs.add(Reference.construct("loc:"+(t+y).hashCode(), t, y));
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("Expection while importing bibtex file: ");
            e.printStackTrace();
            return refs;
        }
        return refs;
    }


    public static void main(String[] args){
        HashSet<Reference> refs = read(System.getenv("CN_HOME")+"/Biblio/Bibtex/CityNetwork.bib");
        for(Reference r:refs){
            System.out.println(r.toString());
        }
        System.out.println(refs.size());
    }



}
