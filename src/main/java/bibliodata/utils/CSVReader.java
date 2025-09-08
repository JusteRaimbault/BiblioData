/**
 * 
 */
package bibliodata.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class CSVReader {


	private static String[] parseLine(String rawLine,String delimiter,String quote){
		//rawLine.replace(quote, ""); can not use split - hand parse
		if (quote.length()==0) return(rawLine.split(delimiter));

		LinkedList<String> acc = new LinkedList<>();
		boolean inside = false;
		StringBuilder currentField = new StringBuilder();
		for(int i=0;i<rawLine.length();i++){
			//System.out.println(rawLine.charAt(i)+" - "+inside+" - "+currentField+ " - "+acc.toArray().length);
			//System.out.println(rawLine.charAt(i) == delimiter.charAt(0));
			//System.out.println(rawLine.charAt(i) == quote.charAt(0));
			//if (quote.length()>0) {
				if (rawLine.charAt(i) == delimiter.charAt(0) && (!inside)) {
					acc.add(currentField.toString());
					currentField = new StringBuilder();
				}
				if ((rawLine.charAt(i) == quote.charAt(0))) {
					inside = !inside;
				}
				if (rawLine.charAt(i) != quote.charAt(0) && rawLine.charAt(i) != delimiter.charAt(0)) {
					currentField.append(rawLine.charAt(i));
				}
			//} else { // with no quote strings should be properly formatted and contain no delimiter
			//	if (rawLine.charAt(i) == delimiter.charAt(0)) {
			//		acc.add(currentField);
			//		currentField = "";
			//	} else {
			//		currentField = currentField + rawLine.charAt(i);
			//	}
			//}
		}
		// do not forget the last field (do not check that outside? ! pb with newlines)
		// TODO fix for newline handling (do not use split but this function also to parse the total file : ~)
		acc.add(currentField.toString());

		String[] res = new String[acc.size()];
		int i = 0;
		for(String s:acc){res[i]=s;i++;}
		return(res);
	}

	/**
	 * Parse a csv file
	 * @param filePath
	 * @param delimiter
	 * @param quote
	 *
	 * TODO quotes are accurately handled with delimiter but not newline -> will fail if \n in a field
	 *
	 * @return
	 */
	public static String[][] read(String filePath,String delimiter,String quote){
		try{
		   BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
		   LinkedList<String[]> listRes = new LinkedList<>();
		   String currentLine = reader.readLine();
		   while(currentLine!= null){
			   if(!currentLine.startsWith("#")) {
				   listRes.addLast(parseLine(currentLine,delimiter,quote));
			   }
		   	   currentLine = reader.readLine();
			   //if(currentLine != null){currentLine = currentLine.replace(quote, "");}
		   }
		   reader.close();
		   //convert list to tab
		   //toArray does not return a matrix
		   String[][] res = new String[listRes.size()][listRes.get(0).length];
		   for(int i=0;i<res.length;i++){res[i]=listRes.get(i);}
		   return res;
		}catch(Exception e){e.printStackTrace();return null;}
	}
	
	public static HashMap<String,String> readMap(String file,String delimiter,String quote){
		HashMap<String,String> res = new HashMap<String,String>();
		String[][] tab = read(file,delimiter,quote);
		for(int r=0;r<tab.length;r++){
			res.put(tab[r][0], tab[r][1]);
		}
		return res;
	}
	
	
	public static void test(){
		//String[][] f = read("data/testIterative/refs_0_keywords.csv","\t","");
		String[][] f = read("data/test2.csv",";","\"");
		for(int i=0;i<f.length;i++){
			for(int j=0;j<f[i].length;j++){
				System.out.println(f[i][j]);
			}
		}
	}

	public static void main(String[] args) {
		test();
	}
	
	
}
