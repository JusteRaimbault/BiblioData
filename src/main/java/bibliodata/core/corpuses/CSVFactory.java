/**
 * 
 */
package bibliodata.core.corpuses;

import java.util.HashMap;

import bibliodata.core.reference.Abstract;
import bibliodata.core.reference.Reference;
import bibliodata.core.reference.Title;
import bibliodata.utils.CSVReader;
import bibliodata.utils.Log;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class CSVFactory implements CorpusFactory {

	private String bibfile;
	
	private int numRefs = -1;
	
	private String citedFolder = "";

	private int idcolumn = 1; // 2nd column by default
	public void setIdcolumn(int i){idcolumn=i;}

	private boolean withHeader = false;
	public void setWithHeader(boolean b){withHeader = b;}

	public CSVFactory(String file){
		bibfile=file;
	}
	
	public CSVFactory(String file,int refs){
		bibfile=file;numRefs=refs;
	}
	
	public CSVFactory(String file,int refs,String cited){
		bibfile=file;numRefs=refs;citedFolder=cited;
	}

	public CSVFactory(String file,boolean wh){
		bibfile=file;withHeader=wh;
	}
	
	/* (non-Javadoc)
	 * @see main.corpuses.CorpusFactory#setup(java.util.HashMap)
	 */
	@Override
	public void setup(HashMap<String, String> options) {
		if(options.keySet().contains("bib-file")){
			bibfile=options.get("bib-file");
		}
	}

	/* (non-Javadoc)
	 * @see main.corpuses.CorpusFactory#getCorpus()
	 */
	@Override
	public OrderedCorpus getCorpus() {
		// assumes a simple csv file : title,ID
		OrderedCorpus res = new OrderedCorpus();
		String[][] refs = CSVReader.read(bibfile, ";","\"");
		Log.stdout("CSV data has "+refs.length+" rows");
		if(refs[0].length>1){
			if(numRefs==-1){numRefs=refs.length;}
			int start = 0;if (withHeader){start=1;}
			for(int i = start;i<numRefs;i++){
				String id = refs[i][idcolumn];
				if(id!="NA"){
					String year = "";
					if (refs[i].length >= 3){year = refs[i][2];} // year assumed as third col
					Reference r = Reference.construct(id,new Title(refs[i][0]),new Abstract(), year);
					res.addReference(r);
					if(citedFolder!=""){//if must construct cited corpus
						r.getBiblio().cited = (new CSVFactory(citedFolder+(new Integer(i+1)).toString(),-1)).getCorpus().references;
					}
				}
			}
		}
		return res;
	}

}
