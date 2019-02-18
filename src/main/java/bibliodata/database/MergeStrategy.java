package bibliodata.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

public class MergeStrategy {


    public HashSet<String> unchangedFields;

    public HashMap<String, Function<Object,Object>> updates;


    public MergeStrategy(HashSet<String> unchanged,HashMap<String, Function<Object,Object>> up){
        unchangedFields=unchanged;updates=up;
    }


    /**

     * @return
     */
    // TODO finish functional interface
    public static MergeStrategy defaultStrategy(){
        HashSet<String> u = new HashSet<String>();

    }


}
