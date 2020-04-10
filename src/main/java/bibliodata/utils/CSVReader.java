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
		LinkedList<String> acc = new LinkedList<>();
		boolean inside = false;
		String currentField = "";
		for(int i=0;i<rawLine.length();i++){
			if (quote.length()>0) {
				if (rawLine.charAt(i) == delimiter.charAt(0) && !inside) {
					acc.add(currentField);
					currentField = "";
				}
				if (rawLine.charAt(i) == quote.charAt(0) && !inside) {
					inside = true;
				}//previous char should be delimiter
				if (rawLine.charAt(i) == quote.charAt(0) && inside) {
					inside = false;
				}
				if (rawLine.charAt(i) != quote.charAt(0) && rawLine.charAt(i) != delimiter.charAt(0)) {
					currentField = currentField + rawLine.charAt(i);
				}
			} else { // with no quote strings should be properly formatted and contain no delimiter
				if (rawLine.charAt(i) == delimiter.charAt(0)) {
					acc.add(currentField);
					currentField = "";
				} else {
					currentField = currentField + rawLine.charAt(i);
				}
			}
		}
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
		String[][] f = read("data/testIterative/refs_0_keywords.csv","\t","");
		for(int i=0;i<f.length;i++){
			for(int j=0;j<f[i].length;j++){
				System.out.println(f[i][j]);
			}
		}
	}
	
	
}
