/**
 * 
 */
package bibliodata.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedList;

import bibliodata.core.corpuses.RISFactory;
import bibliodata.core.reference.Abstract;
import bibliodata.core.reference.BibTeXParser;
import bibliodata.core.reference.Reference;
import bibliodata.core.reference.Title;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class RISReader {

	
	/**
	 * Constructs a set of refs from RIS file.
	 * 
	 * Used to reuse bib files.
	 * 
	 * @param filePath
	 * @return
	 */
	public static HashSet<Reference> read(String filePath,int size){
		HashSet<Reference> refs = new HashSet<Reference>();
		try{
		   BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
		   String currentTitle="",currentENTitle="",currentAbstract="",currentYear="",currentID="";
		   HashSet<String> currentKeywords=new HashSet<String>();
		   HashSet<Reference> currentCitedGhostRefs=new HashSet<Reference>();
		   String currentLine = reader.readLine();
		  
		   while(currentLine!= null){
			   // check new ref criterium : TY -
			   if(currentLine.startsWith("TY")&&currentTitle.length()>0){
				   //
				   //
				   Reference newRef = Reference.construct(currentID, new Title(currentTitle), new Abstract(currentAbstract), currentYear);
				   for(String s:currentKeywords){newRef.setKeyword(s);};
				   for(Reference r:currentCitedGhostRefs){newRef.setCited(r);}
				   
				   currentKeywords.clear();
				   currentCitedGhostRefs.clear();
				   currentAbstract="";currentTitle="";currentENTitle="";currentYear="";currentID="";
				   
				   refs.add(newRef);  
				   
				   if(refs.size()==size){break;}
			   }
			   if(currentLine.startsWith("AB")){
				   String[] t = currentLine.split("AB  - ");
				   if(t.length>1){currentAbstract=t[1];}
			   }
			   if(currentLine.startsWith("T1")){
				   String[] t = currentLine.split("T1  - ");
				   if(t.length>1){currentTitle=t[1];}
			   }
			   if(currentLine.startsWith("TT")){
				   String[] t = currentLine.split("TT  - ");
				   if(t.length>1){currentENTitle=t[1];}
			   }
			   if(currentLine.startsWith("PY")){
				   String[] t = currentLine.split("PY  - ");
				   if(t.length>1){currentYear=t[1];}
			   }
			   if(currentLine.startsWith("ID")){
				   String[] t = currentLine.split("ID  - ");
				   if(t.length>1){currentID=t[1];}
			   }
			   if(currentLine.startsWith("KW")){
				   String[] t = currentLine.split("KW  - ");
				   if(t.length>1){currentKeywords.add(t[1]);}
			   }
			   if(currentLine.startsWith("BI")){
				   String[] t = currentLine.split("BI  - ");
				   if(t.length>1){
					   // cited refs may not be bib formatted : catch and do nothing
					   try{
					      currentCitedGhostRefs.add(BibTeXParser.parseBibtexString(t[1]));   
					   }catch(Exception e){System.out.println("parsing bib : "+t[1]+" is not a bibtex string");}
				   }
			   }
			   currentLine = reader.readLine();
		   }
		   
		   //add the last ref
		   if(refs.size()<size||size==-1){
			   Reference newRef = Reference.construct(currentID, new Title(currentTitle), new Abstract(currentAbstract), currentYear);
			   // add additionary fields by hand. Dirty dirty, must have set/get methods
			   newRef.setKeywords(currentKeywords);
			   newRef.setCited(currentCitedGhostRefs); // cited set should not be null
			   newRef.getTitle().en_title=currentENTitle; // FIXME this is absurd to have setters/getters at the first level and being mutable at the second level
			   refs.add(newRef);
		   }
		   
		   
		   
		   reader.close();
		   
		}catch(Exception e){e.printStackTrace();return null;}
		return refs;
	}
	
	
	
	public static void main(String[] args){
		HashSet<Reference> refs = read(System.getenv("CS_HOME")+"/Cybergeo/cybergeo20/Data/bib/fullbase_refsAsBib.ris",3);
		for(Reference r:refs){
			System.out.println(r);
			for(Reference t:r.getCited()){
				Log.stdout(t.toString());
			}
		}
	}
	
	
	
	
}
