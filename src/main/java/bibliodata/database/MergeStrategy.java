package bibliodata.database;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static com.mongodb.client.model.Updates.set;

public class MergeStrategy {


    public HashSet<String> unchangedFields;

    public HashMap<String, Function<Object[],Object>> updates;


    public MergeStrategy(HashSet<String> unchanged,HashMap<String, Function<Object[],Object>> up){
        unchangedFields=unchanged;updates=up;
    }


    /**
     *
     * @return
     */
    public static MergeStrategy defaultStrategy(){
        HashSet<String> unchanged = new HashSet<String>();
        unchanged.add("_id");
        unchanged.add("id");

        HashMap<String,Function<Object[],Object>> updates = new HashMap();

        // FIXME totally unsafe
        //  + should not try func prog with mutables
        Function<Object[],Object> or = (Object[] o) -> {
            List<Bson> currentupdates = ((List<Bson>) o[0]);
            Document existing = ((Document) o[1]);
            Document newdoc = ((Document) o[2]);
            currentupdates.add(set("citingFilled",newdoc.getBoolean("citingFilled")||existing.getBoolean("citingFilled")));
            return(o);
        };
        updates.put("citingFilled",or);


        // concatenation of origins
        Function<Object[],Object> conc = (Object[] o) -> {
            List<Bson> currentupdates = ((List<Bson>) o[0]);
            Document existing = ((Document) o[1]);
            Document newdoc = ((Document) o[2]);
            if(existing.getString("origin").length()>0){
                if(!existing.getString("origin").contains(newdoc.getString("origin"))) {
                    currentupdates.add(set("origin", existing.getString("origin") + ";" + newdoc.getString("origin")));
                }else{
                    currentupdates.add(set("origin",newdoc.getString("origin")));
                }
            }
            return(o);
        };
        updates.put("origin",conc);

        // max for depths
        Function<Object[],Object> max = (Object[] o) -> {
            List<Bson> currentupdates = ((List<Bson>) o[0]);
            Document existing = ((Document) o[1]);
            Document newdoc = ((Document) o[2]);
            if(existing.getInteger("depth")<newdoc.getInteger("depth")){
                currentupdates.add(set("depth",newdoc.getInteger("depth")));
            }
            return(o);
        };
        updates.put("depth",max);


        return(new MergeStrategy(unchanged,updates));
    }


}
