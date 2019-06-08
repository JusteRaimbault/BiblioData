package bibliodata.core.reference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import bibliodata.utils.Log;
import org.apache.commons.lang3.StringUtils;

/**
 * Class representing references.
 * 
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class Reference {

	/**
	 * Static management of all references
	 */

	/**
	 * Static set of all references.
	 */
	private static final HashMap<Reference,Reference> references = new HashMap<Reference,Reference>();

	/**
	 * get a copy of HashSet of current references
	 *
	 * @return hashset containing current references
	 */
	public static HashSet<Reference> getReferences(){return(new HashSet<>(references.keySet()));};

	/**
	 * current number of references
	 * @return number of references
	 */
	public static int getNumberOfReferences(){return(references.keySet().size());}

	/**
	 * forget a reference
	 * @param r remove a given reference from map of all refs
	 *
	 */
	public static void removeReference(Reference r){references.remove(r);}

	/**
	 * Empty ref - the way equals is written imposes the weird id
	 */
	public static final Reference empty = new Reference("","-----------EMPTY-----------");
	public boolean isEmpty(){return(this.equals(empty));}

	/**
	 * Secondary id
	 *  - for exampld UUID retrieved from mendeley.
	 */
	//public String id;
	private String secondaryId;
	public String getSecondaryId(){return(secondaryId);}
	public void setSecondaryId(String secid){secondaryId=secid;}

	/**
	 * Unique id of the ref
	 */
	private String id;
	public String getId(){return(id);}
	//public void setId(String newid) // Should not change the id of a ref, as used for objects managements etc
	public void setId(String newid){
		//throw new RuntimeException("Cannot change the id of a ref");
		// but allow it - in the case of ghost references with no id, when title only is used for hashcode and equals
		id=newid;
	}
	public boolean hasId() {
		if(id==null){return(false);} // FIXME id should NEVER be null
		if(id.length()==0){return(false);}
		// else any non empty string could be considered as an id - could include here regex specification
		return(true);
	}

	/**
	 * Title
	 */
	private Title title;
	public Title getTitle(){return(title);}
	public void setTitle(Title newtitle){title=newtitle;}
	public void setTitle(String newtitle){title=new Title(newtitle);}


	/**
	 * Authors
	 */
	private HashSet<String> authors = new HashSet<>();
	public HashSet<String> getAuthors(){
		if(authors==null){return(new HashSet<>());}
		return(authors);
	}
	/**
	 * Set keywords from an existing collection.
	 *
	 * @param a collection of authors to add
	 */
	public void setAuthors(Collection<String> a){
		// current set of authors assumed existing ; but creates it of called from ghost ref e.g.
		//if(authors==null){authors=new HashSet<>();} // cannot be null as initialized
		for(String s:a){authors.add(s);}
	}

	public void setAuthor(String a){authors.add(a);}

	/**
	 * Authors as string.
	 * @return
	 */
	public String getAuthorString(){
		try{
			StringBuilder sb = new StringBuilder();
			for(String a:authors){sb.append(","+a);}
			String res = sb.toString();
			if(res.length()>0){res = res.substring(0, res.length()-1);}
			return res;
			// FIXME why could there be an exception here ? uninitialized authors ?
		}catch(Exception e){return "";}
	}




	/**
	 * Abstract. (abstract is a java keyword)
	 */
	private Abstract resume;
	public Abstract getResume(){return(resume);}
	public void setResume(String newresume){resume = new Abstract(newresume);}

	/**
	 * Keywords
	 */
	private HashSet<String> keywords = new HashSet<>();

	public HashSet<String> getKeywords() {return keywords;}

	public void setKeyword(String k){keywords.add(k);}

	/**
	 * Set keywords from an existing set.
	 *
	 * @param k collection of keywords to add
	 */
	public void setKeywords(Collection<String> k){
		// current set of authors assumed existing ; but creates it of called from ghost ref e.g.
		//if(keywords==null){keywords=new HashSet<>();}
		keywords.addAll(k);
	}

	/**
	 * Keywords as string.
	 *
	 * @return aggregated string of keywords
	 */
	public String getKeywordString(){
		try{
			StringBuilder sb = new StringBuilder();
			for(String a:keywords){sb.append(";"+a);}
			String res = sb.toString();
			if(res.length()>0){res = res.substring(0, res.length()-1);}
			return res;
		}catch(Exception e){return "";}
	}

	/**
	 * publication year
	 */
	private String year;
	public String getYear(){return(year);}
	public void setYear(String newyear){year=newyear;}


	/**
	 * publication date - not used
	 */
	private String date = "";
	public String getDate(){return(date);}
	public void setDate(String newdate){date=newdate;}

	
	/**
	 * Refs citing this ref
	 */
	private HashSet<Reference> citing = new HashSet<>();
	public HashSet<Reference> getCiting(){return(citing);}
	public void setCiting(Reference r){citing.add(r);}
	public void setCiting(HashSet<Reference> refs){for(Reference r:refs){setCiting(r);}}
	public void setCiting(Collection<Reference> refs){setCiting(new HashSet(refs));}


	private boolean citingFilled = false;
	public boolean isCitingFilled(){return(citingFilled);}
	public void setCitingFilled(boolean b){citingFilled=b;}

	/**
	 * relative vertical depth in the citation network
	 */
	private int depth = -1;
	public int getDepth(){return(depth);}
	public void setDepth(int newdepth){depth=newdepth;}

	/**
	 * Horizontal depth -> as a map since the reference can come from different contexts :
	 *   - keyword request (from several simultaneously) map reqString -> depth
	 *   - ~order in citing : map citing ID -> depth~
	 */
	private HashMap<String,Integer> horizontalDepth = new HashMap<>();
	public int getHorizontalDepth(String key){if(horizontalDepth.containsKey(key)){return(horizontalDepth.get(key).intValue());}else{return(-1);}}
    public HashMap<String,Integer> getHorizontalDepthMap(){return(horizontalDepth);}
	public void setHorizontalDepth(String key,int value){
		if(horizontalDepth!=null){
			horizontalDepth.put(key,value);
		}
	}


	/**
	 * where did the reference originated from in the case of multiple corpuses
	 */
	private String origin = "";
	public String getOrigin(){return(origin);}
	public void setOrigin(String newOrigin){origin = newOrigin;}


	/**
	 * Bibliography
	 */
	private Bibliography biblio = new Bibliography();
	public Bibliography getBiblio(){return(biblio);}
	public void setBiblio(Bibliography newbib){biblio = newbib;}
	public HashSet<Reference> getCited(){return(biblio.cited);}
	public void setCited(Reference r){biblio.cited.add(r);}
	public void setCited(Collection<Reference> c){biblio.cited.addAll(c);}

	
	/**
	 * Free attributes, stored under the form <key,value>
	 *     // FIXME privatize
	 */
	private HashMap<String,String> attributes = new HashMap<>();
	public HashMap<String,String> getAttributes(){return(attributes);}
	/**
	 * Add an attribute to the Reference
	 * @param key attr name
	 * @param value attr value
	 */
	public void setAttribute(String key,String value){
		if(attributes==null){attributes=new HashMap<String,String>();}
		attributes.put(key, value);
	}


	/**
	 * Get attribute (empty if attributes not set [!! unsecure] or attribute is not in the attribute table)
	 * @param key attr name
	 * @return value of attribute if exists, empty string otherwise
	 */
	public String getAttribute(String key){
		if(!attributes.containsKey(key)){return("");}
		return(attributes.get(key));
	}


	private long timestamp=Log.currentTimestamp(); // ts set when the ref is initially created, then modified when citingFilled achieved for example
	public long getTimestamp(){return(timestamp);}
	public void setTimestamp(long newts){timestamp=newts;}


	// graph visiting variables -> not performant if has to reset at each call ? ok if origin is small ; otherwise in O(N2) - in that case use a map for linear time
	private boolean visited = false;
	public void setVisited(boolean b){visited = b;}
	public boolean isVisited(){return(visited);}





	/**
	 * Constructor
	 * 
	 * Authors and keywords have to be populated after (more simple ?)
	 * 
	 * @param t title
	 * @param r abstract
	 */
	public Reference(String i, String secid,Title t,Abstract r,String y){
		id=i;
		secondaryId=secid;
		title=t;
		resume=r;
		year=y;
	}
	
	/**
	 * Ghost constructor
	 */
	public Reference(String t){
		title=new Title(t);
	}
	
	/**
	 * Ghost constructor with ID.
	 * 
	 * @param t title
	 * @param schid scholar id as String
	 */
	public Reference(String t,String schid){
		title=new Title(t);
		id=schid;
	}
	
	
	
	/**
	 * Static constructor used to construct objects only one time.
	 *
	 * @param id : scholar ID
	 * @param t : title
	 * @param r : resume
	 * @param y : year
	 * @return the Reference object, ensuring overall unicity through HashConsing
	 */
	public static Reference construct(String id,Title t,Abstract r,String y){
		Reference ref = new Reference(t.title,id);
		if(references.containsKey(ref)){
			Reference existingRef = references.get(ref);
			//if(i.length()>0){existingRef.id=i;}
			if(r.resume.length()>0){existingRef.resume=r;}
			if(y.length()>0){existingRef.year=y;}
			if(id.length()>0){existingRef.id=id;}
			
			return existingRef;
		}else{
			Reference newRef = new Reference(id,"",t,r,y);
			//put in map
			//newRef.id=new Integer(references.size()).toString();
			references.put(newRef, newRef);
			return newRef;
		}
	}

	public static Reference construct(String id,String title){
		return construct(id,new Title(title),new Abstract(),"");
	}

	public static Reference construct(String id,String title, String year){
		return construct(id,new Title(title),new Abstract(),year);
	}

	/**
	 * construct a ref and merge attributes (following specific merging rules - as in [[MongoConnection]] we are strongly
	 *  limited by java and the lack of functional programming - so merging strategies are not parametrized as arguments
	 *
	 *  -- also this manual hashconsing is heavy - should we go full scala asap ? -- java legacy is finally a pain -- see NetLogo nighmares
	 * @param id id of the ref
	 * @param title String title
	 * @param year year
	 * @param attributes hashmap of attributes as strings
	 * @return unique reference object with the given id
	 *
	 * TODO this function is key in the merging of refs during consolidation for example -> updating of citing should be accounted for (e.g. merging older refs with newer should add the newly collected citing)
	 *
	 *
	 */
	public static Reference construct(String id,String title, String year,HashMap<String,String> attributes){
		Reference res = construct(id,title,year);

		// merge attributes
		// first add default in attr map if not present - fuck to not have the getOrElse
		if(!attributes.containsKey("depth")||attributes.get("depth").length()==0){attributes.put("depth",Integer.toString(Integer.MAX_VALUE));}
		if(!attributes.containsKey("priority")||attributes.get("priority").length()==0){attributes.put("priority",Integer.toString(Integer.MAX_VALUE));}
		if(!attributes.containsKey("horizontalDepth")){attributes.put("horizontalDepth","");}
		if(!attributes.containsKey("citingFilled")){attributes.put("citingFilled","false");}


		// FIXME depth is not a raw attribute ! -> but better for csv export ?
		if(res.getAttribute("depth").length()>0){res.attributes.put("depth",Integer.toString(Math.min(Integer.parseInt(res.getAttribute("depth")),Integer.parseInt(attributes.get("depth")))));} else {res.attributes.put("depth",attributes.get("depth"));}
		if(res.getAttribute("priority").length()>0){res.attributes.put("priority",Integer.toString(Math.min(Integer.parseInt(res.getAttribute("priority")),Integer.parseInt(attributes.get("priority")))));} else {res.attributes.put("priority",attributes.get("priority"));}
		// merging horizdepths : reparse and merge hashmaps - ultra dirty - should have a generic trait Mergeable and different implementations
		if(res.getAttribute("horizontalDepth").length()>0){
			res.attributes.put("horizontalDepth",
					mergeHorizDepths(res.getAttribute("horizontalDepth"),attributes.get("horizontalDepth")));
		}else{
			res.attributes.put("horizontalDepth",attributes.get("horizontalDepth"));
		}
		// FIXME citingFilled is not a raw attribute
		if(res.getAttribute("citingFilled").length()>0){res.setAttribute("citingFilled",Boolean.toString(Boolean.parseBoolean(res.getAttribute("citingFilled"))||Boolean.parseBoolean(attributes.get("citingFilled"))));}else{res.setAttribute("citingFilled",attributes.get("citingFilled"));}

		// timestamp : depending on if citingFilled is true or not should choose ? anyway take the latest
		// FIXME timestamp also not an attribute ? simpler to export as csv
		if (attributes.containsKey("timestamp")) {
			if (res.getAttribute("timestamp").length() > 0) {
				res.setAttribute("timestamp", Integer.toString(Math.max(Integer.parseInt(res.getAttribute("timestamp")), Integer.parseInt(attributes.get("timestamp")))));
			} else {
				res.setAttribute("attribute", attributes.get("timestamp"));
			}
		}

		return(res);
	}

	
	public static Reference construct(String schID){
		return construct(schID,new Title(""),new Abstract(),"");
	}
	
	
	/**
	 * Construst from ghost.
	 */
	public static Reference construct(GhostReference ghost,String schID){
		if (ghost==null){return(null);}
		Reference materializedRef = construct(schID,ghost.getTitle(),ghost.getResume(),ghost.getYear());
		// copy keywords and authors
		materializedRef.setKeywords(ghost.getKeywords());
		materializedRef.setAuthors(ghost.getAuthors());
		return materializedRef;
	}
	
	//
	//Materialize a ghost ref.
	 //
	 // @param ghost
	 // @return
	 ///
	//public static Reference materialize(GhostReference ghost){
	//	return construct(ghost,"");
	//}

	/**
	 * Materialize a ghost ref with the given id
	 * @param ghost ghost ref to materialize
	 * @param schID id
	 * @return
	 */
	public static Reference materialize(GhostReference ghost,String schID){
		return construct(ghost,schID);
	}




	private static String mergeHorizDepths(String hd1,String hd2){
		HashMap<String,Integer> hd = new HashMap<>();
		// do not resole conflict ? yes otherwise just concatenation would be enough
		if (hd1.length()>0) {
			for(String k1:hd1.split(",")){String[] k1s = k1.split(":");hd.put(k1s[0],Integer.parseInt(k1s[1]));}
		}
		if (hd2.length()>0) {
			for (String k2 : hd2.split(",")) {
				String[] k2s = k2.split(":");
				if (hd.containsKey(k2s[0])){
					hd.put(k2s[0],Math.min(Integer.parseInt(k2s[1]),hd.get(k2s[0])));
				}
				else {
					hd.put(k2s[0],Integer.parseInt(k2s[1]));
				}
			}
		}

		// hd.toSeq.map{_._1+":"+_._2}.mkString(",")
		String res = "";
		for (String k: hd.keySet()){res=res+","+k+":"+hd.get(k).toString();}
		return(res.substring(1));
	}
	
	

	



	/**
	 * propagates depth into the citation network
	 */
	/*
	public void setDepthRec(int newDepth, HashSet<Reference> origin){

		// FIXME "bug" (let say indesirable behavior) here as ref in the first layer for which depth has not been set will not be gone through if done sequentially
		// => call the function only with a consistent first layer

		//if (!visitedDepth) {

		depth = Math.max(depth, newDepth);
		//visitedDepth = true;
		HashSet<Reference> newOrigSeq = new HashSet<Reference>(origin);
		newOrigSeq.add(this);
		for (Reference c : citing) {
			if(!origin.contains(c)) {
				c.setDepthRec(depth - 1, newOrigSeq);
			}
		}
	}*/

	public void setDepthRec(int newdepth){
		if (!visited){
			depth = Math.max(depth, newdepth);
			visited = true;
			for (Reference c : citing) {c.setDepthRec(newdepth - 1);}
		}
	}

	/**
	 * entry point for setting depth
	 * @param newDepth
	 */
	public void setDepth0(int newDepth){
		//Log.stdout("Set depth : "+id);
		for(Reference r: Reference.getReferences()){r.setVisited(false);}
		setDepthRec(newDepth);
	}

	/**
	 * set and propagates horizontal depth into the citation network
	 *   - was not secure to reciprocal citations => indeed happens : add provenance ? could be loops - needs a visited
	 *
	 */
	// FIXME this function DOES NOT WORK - why is an other matter ... - looks like id are messing around and infinite loops are created
	/*public void setHorizontalDepthRec(String origin,int depth,HashSet<String> chain){

		Log.stdout("Set hdepth : "+getId()+" - d = "+depth+" - "+chain.size());

		if (getHorizontalDepthMap().containsKey(origin)) {
			setHorizontalDepth(origin, new Integer(Math.min(depth, getHorizontalDepth(origin))));
		} else {
			setHorizontalDepth(origin, new Integer(depth));
		}

		HashSet<String> newOrigSeq = new HashSet<>(chain);
		newOrigSeq.add(getId());

		Log.stdout("  rec calls : "+getCiting().size());
		for (Reference c : getCiting()) {
			if(!chain.contains(c.getId())) {
				Log.stdout(" --- from "+getId());
				StringBuilder sb = new StringBuilder();for(String s:chain){sb.append(s+" ; ");}
				Log.stdout(" --- chain is "+sb.toString());
				c.setHorizontalDepthRec(origin, depth,newOrigSeq);
			}
		}

	}*/

	public void setHorizontalDepthRec(String origin, int newdepth) {
		if (!visited){
			if (getHorizontalDepthMap().containsKey(origin)) {
				setHorizontalDepth(origin, new Integer(Math.min(newdepth, getHorizontalDepth(origin))));
			} else {
				setHorizontalDepth(origin, new Integer(newdepth));
			}
			visited = true;
			for (Reference c : getCiting()) {
				setHorizontalDepthRec(origin,newdepth);
			}
		}
	}


	public void setHorizontalDepth0(String origin, int newdepth){
		//Log.stdout(" ----- Set hdepth : "+id+" ------");
		for(Reference r :Reference.getReferences()){r.setVisited(false);}
		setHorizontalDepthRec(origin,newdepth);
	}

	
	
	/**
	 * Override hashcode to take account of only ID.
	 */
	public int hashCode(){
		/**
		 * dirty, has to go through all table to find through Levenstein close ref
		 * that way hashconsing may be (is surely) suboptimal -> in O(n^2)
		 * 
		 * If scholarID is set, use it -> O(n) thanks to O(1) for hashcode computation
		 */
		
		if(id!=null&&id!=""){
			return id.hashCode();
		}else{
			for(Reference r:references.keySet()){if(r.equals(this)){return r.title.title.hashCode();}}
			return this.title.title.hashCode();
		}
	}
	
	/**
	 * Idem with equals
	 */
	public boolean equals(Object o){
		if(!(o instanceof Reference)){return false;}
		else{
			Reference r = (Reference) o;
			if(r.id!=null&&r.id!=""&&id!=null&&id!=""){
				return r.id.equals(id);
			}
			else{
				return (StringUtils.getLevenshteinDistance(StringUtils.lowerCase(r.title.title),StringUtils.lowerCase(title.title))<4);			
			}
		}
	}
	
	
	/**
	 * Override to string
	 */
	public String toString(){
		return "Ref "+id+" - t : "+title+" - year : "+year+" - Cited by : "+citing.size()+" - authors : "+getAuthorString()+" - keywords : "+getKeywordString();
	}
	
}
