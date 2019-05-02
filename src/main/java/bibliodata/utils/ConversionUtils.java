package bibliodata.utils;

import java.util.Collection;
import java.util.LinkedList;

public class ConversionUtils {


    public static String[] toArray(LinkedList<String> list){
        String[] res = new String[list.size()];
        int i=0;for(String s:list){res[i]=s;i++;}
        return(res);
    }

    /*
    public static String[][] toArray(Collection<Collection<String>> col){
        String[][] res = new String[col.size()][];
        for(int i =0;i<col.size();i++){res[i]=s;}
        return(res);
    }*/


}
