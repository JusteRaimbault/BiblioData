package bibliodata;

import bibliodata.core.CitationNetworkRetriever;
import bibliodata.core.DatabaseManager;
import bibliodata.core.KeywordsRequest;
//import bibliodata.mendeley.AbstractRetriever;
import bibliodata.core.AbstractSetRetriever;

public class Main {


    public static String usage = "Usage : java -jar bibliodata.jar $OPTION [...]; where $OPTION in {--database,--citation,--abstracts,--abstract,--keywords}";


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
            //case "--abstract" : // FIXME no use ?
            //    AbstractRetriever.main(tail);break;
            case "--keywords" :
                KeywordsRequest.main(tail);break;

        }

    }


}
