
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

    private static boolean logips = true;

    public static void setLogips(boolean logips) {
        Context.logips = logips;
    }

    //@Contract(pure = true)
    public static boolean getLogips() {return(logips);}


    /**
     * Mongo
     */

    private static String referencesCollection = "references";
    public static void setReferencesCollection(String col){Context.referencesCollection = col;}
    public static String getReferencesCollection() {return(referencesCollection);}

    private static String citationsCollection = "links";
    public static void setCitationsCollection(String col){Context.citationsCollection=col;}
    public static String getCitationsCollection() {return(Context.citationsCollection);}

    private static String netstatCollection = "netstat";
    public static void setNetstatCollection(String col){Context.netstatCollection=col;}
    public static String getNetstatCollection() {return(Context.netstatCollection);}

    private static String mongoHost = "127.0.0.1";
    public static void setMongoHost(String host){Context.mongoHost=host;}
    public static String getMongoHost(){return(Context.mongoHost);}

    private static int mongoPort = 27017;
    public static void setMongoPort(int port){Context.mongoPort=port;}
    public static int getMongoPort(){return(Context.mongoPort);}

    private static String centralDatabase = "consolidated";
    public static void setCentralDatabase(String db){centralDatabase = db;}
    public static String getCentralDatabase(){return(centralDatabase);}

    /**
     * scholar api
     */
    private static int scholarMaxRequests = 1000;
    public static int getScholarMaxRequests() {return(scholarMaxRequests);}


    public static int getMaxHorizontalDepth(){return(scholarMaxRequests);}


}
