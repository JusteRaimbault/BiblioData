package bibliodata;

import bibliodata.core.*;
//import bibliodata.mendeley.AbstractRetriever;


public class Main {


    public static final String usage = "Usage : java -jar bibliodata.jar $OPTION [...]\n"+
            "where $OPTION in {--database,--citation,--abstracts,--abstract,--keywords}";


    /**
     * final steps always done at exception ?
     */
    public static void close() {

    }

    public static void main(String[] args) {

        if(args.length==0){System.out.println(usage);}

        String mode = args[0];
        String[] tail = new String[args.length-1];
        for(int i=0;i<tail.length;i++){tail[i]=args[i+1];}

        switch (mode) {
            case "--database" :
                DatabaseManager.main(tail);break;
            case "--citation" :
                CitationNetworkRetriever.main(tail);break;
            case "--abstracts" :
                AbstractSetRetriever.main(tail);break;
            case "--keywords" :
                KeywordsRequest.main(tail);break;
            case "--utils":
                Utils.main(tail);break;

        }

    }


}
