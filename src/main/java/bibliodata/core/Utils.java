package bibliodata.core;

import bibliodata.core.corpuses.Corpus;

public class Utils {


    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println(
                    "Usage : --utils\n" +
                            "| --corpus2csv $INFILE $OUTFILEPREFIX\n"
            );
        } else {

            String action = args[0];

            if (action.equals("--corpus2csv")) {
                String infile = args[1];
                String outfile = args[2];
                Corpus.fromNodeFile(infile,"").csvExport(outfile,false);
            }
        }
    }


}
