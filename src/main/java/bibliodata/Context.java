
package bibliodata;


//import org.jetbrains.annotations.Contract;

/**
 * Class for global options
 *
 *  -> ~ ; bad practice. at least enforce setters / getters
 */
public class Context {

    /**
     * IPs logging
     */

    private static boolean logips = false;

    public static void setLogips(boolean logips) {
        Context.logips = logips;
    }

    //@Contract(pure = true)
    public static boolean getLogips() {return(logips);}


    /**
     * mongo collections
     */

    private static String referencesCollection = "references";
    public static void setReferencesCollection(String col){Context.referencesCollection = col;}
    public static String getReferencesCollection() {return(referencesCollection);}

    private static String citationsCollection = "citations";
    public static void setCitationsCollection(String col){Context.citationsCollection=col;}
    public static String getCitationsCollection() {return(Context.citationsCollection);}

    private static String netstatCollection = "netstat";
    public static void setNetstatCollection(String col){Context.netstatCollection=col;}
    public static String getNetstatCollection() {return(Context.netstatCollection);}

}
