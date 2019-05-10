/**
 * 
 */
package bibliodata.core.reference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import bibliodata.mendeley.MendeleyAPI;

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
	 * FIXME making the consing map private must be done checking no mutable operation operated on it from outside !
	 * @return
	 */
	public static HashSet<Reference> getReferences(){return(new HashSet<>(references.keySet()));};

	/**
	 * current number of references
	 * @return
	 */
	public static int getNumberOfReferences(){return(references.keySet().size());}

	/**
	 * forget a reference
	 * @param r
	 */
	public static void removeReference(Reference r){references.remove(r);}


	/**
	 * Dynamic fields
	 * 
	 */
	
	/**
	 * UUID retrieved from mendeley.
	 */
	public String id;
	
	/**
	 * Google scholar UUID (cluster)
	 */
	public String scholarID;
	
	/**
	 * Title
	 */
	public Title title;
	
	/**
	 * Authors
	 * Useful ?
	 */
	public HashSet<String> authors;
	
	/**
	 * Abstract. (abstract is a java keyword)
	 */
	public Abstract resume;
	
	/**
	 * Keywords
	 */
	public HashSet<String> keywords;
	
	/**
	 * publication year
	 */
	public String year;

	/**
	 * publication date
	 */
	public String date;
	
	
	/**
	 * Refs citing this ref
	 */
	public HashSet<Reference> citing;
	
	public boolean citingFilled;

	/**
	 * relative vertical depth in the citation network
	 */
	public int depth;

	/**
	 * Horizontal depth -> as a map since the reference can come from different contexts :
	 *   - keyword request (from several simultaneously) map reqString -> depth
	 *   - order in citing : map citing ID -> depth
	 *   // FIXME is this useful ?
	 */
	public HashMap<String,Integer> horizontalDepth;


	/**
	 * where originated from in the case of multiple corpuses
	 */
	public String origin;

	/**
	 * Bibliography
	 */
	public Bibliography biblio;
	
	
	/**
	 * Free attributes, stored under the form <key,value>
	 */
	public HashMap<String,String> attributes;
	
	
	
	/**
	 * Constructor
	 * 
	 * Authors and keywords have to be populated after (more simple ?)
	 * 
	 * @param t title
	 * @param r abstract
	 */
	public Reference(String i,Title t,Abstract r,String y,String schID){
		id=i;
		title=t;
		resume=r;
		year=y;
		date="";
		scholarID=schID;
		authors = new HashSet<String>();
		keywords = new HashSet<String>();
		citing=new HashSet<Reference>();
		biblio=new Bibliography();
		attributes = new HashMap<String,String>();
		citingFilled = false;
		depth = -1;
		horizontalDepth=new HashMap<String,Integer>();
		origin="";
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
		scholarID=schid;
	}
	
	
	
	/**
	 * Static constructor used to construct objects only one time.
	 * 
	 * @param i : id
	 * @param t : title
	 * @param r : resume
	 * @param y : year
	 * @param schID : scholar ID
	 * @return the Reference object, ensuring overall unicity through HashConsing
	 */
	public static Reference construct(String i,Title t,Abstract r,String y,String schID){
		Reference ref = new Reference(t.title,schID);
		if(references.containsKey(ref)){
			Reference existingRef = references.get(ref);
			//System.out.println("DUPLICATE : "+existingRef);
			//System.out.println("BY  : "+ref);
			//override existing records if not empty fields provided --> the function can be used as a table updater --
			//ref in table has thus always the latest requested values. NO ?		
			if(i.length()>0){existingRef.id=i;}
			if(r.resume.length()>0){existingRef.resume=r;}
			if(y.length()>0){existingRef.year=y;}
			if(schID.length()>0){existingRef.scholarID=schID;}
			
			return existingRef;
		}else{
			Reference newRef = new Reference(i,t,r,y,schID);
			//put in map
			//newRef.id=new Integer(references.size()).toString();
			references.put(newRef, newRef);
			return newRef;
		}
	}

	public static Reference construct(String id,String title){
		return construct("",new Title(title),new Abstract(),"",id);
	}

	public static Reference construct(String id,String title, String year){
		return construct("",new Title(title),new Abstract(),year,id);
	}

	/**
	 * construct a ref and merge attributes (following specific merging rules - as in [[MongoConnection]] we are strongly
	 *  limited by java and the lack of functional programming - so merging strategies are not parametrized as arguments
	 *
	 *  -- also this manual hashconsing is heavy - should we go full scala asap ? -- java legacy is finally a pain -- see NetLogo nighmares
	 * @param id
	 * @param title
	 * @param year
	 * @param attributes
	 * @return
	 */
	/*public static Reference construct(String id,String title, String year,HashMap<String,String> attributes){
		Reference res = construct(id,title,year);
		// merge attributes
		// first add default in attr map if not present - fuck to not have the getOrElse
		if(!attributes.containsKey("depth")){attributes.put("depth",Integer.toString(Integer.MAX_VALUE));}
		if(!attributes.containsKey("priority")){attributes.put("priority",Integer.toString(Integer.MAX_VALUE));}
		if(!attributes.containsKey("horizontalDepth")){attributes.put("horizontalDepth","");}
		if(res.getAttribute("depth").length()>0){Math.min(Integer.parseInt(res.attributes.get("depth")),Integer.parseInt(attributes.get("depth")));} else {res.attributes.put("depth",attributes.get("depth"));}
		if(res.getAttribute("priority").length()>0){Math.min(Integer.parseInt(res.attributes.get("priority")),Integer.parseInt(attributes.get("priority")));} else {res.attributes.put("priority",attributes.get("priority"));}
		// merging horizdepths : reparse and merge hashmaps - ultra dirty - should have a generic trait Mergeable and different implementations
		// TODO
	}*/

	
	public static Reference construct(String schID){
		return construct("",new Title(""),new Abstract(),"",schID);
	}
	
	
	/**
	 * Construst from ghost.
	 */
	public static Reference construct(GhostReference ghost,String schID){
		// null ghost is catch by nullPointerException -- ¡¡ DIRTY !!
		Reference materializedRef = null;
		try{
			materializedRef = construct(ghost.id,ghost.title,ghost.resume,ghost.year,schID);
			// copy keywords and authors
			materializedRef.setKeywords(ghost.keywords);
			materializedRef.setAuthors(ghost.authors);
		}catch(Exception e){}
		return materializedRef;
	}
	
	/**
	 * Materialize a ghost ref.
	 * 
	 * @param ghost
	 * @return
	 */
	public static Reference materialize(GhostReference ghost){
		return construct(ghost,"");
	}

	/**
	 * Materialize a ghost ref with the given id
	 * @param ghost
	 * @param schID
	 * @return
	 */
	public static Reference materialize(GhostReference ghost,String schID){
		return construct(ghost,schID);
	}


	/**
	 * Add an attribute to the Reference
	 * @param key
	 * @param value
	 */
	public void addAttribute(String key,String value){
		if(attributes==null){attributes=new HashMap<String,String>();}
		attributes.put(key, value);
	}

	/**
	 * Get attribute (empty if attributes not set [!! unsecure] or attribute is not in the attribute table)
	 * @param key
	 * @return
	 */
	public String getAttribute(String key){
		if(attributes==null||!attributes.containsKey(key))return "";
		return attributes.get(key);
	}
	
	
	/**
	 * Set keywords from an existing collection.
	 * 
	 * @param a
	 */
	public void setAuthors(Collection<String> a){
		// current set of authors assumed existing ; but creates it of called from ghost ref e.g.
		if(authors==null){authors=new HashSet<String>();}
		for(String s:a){authors.add(s);}
	}

	public HashSet<String> getAuthors(){
		if(authors==null){return(new HashSet<>());}
		return(authors);
	}
	
	/**
	 * Set keywords from an existing set.
	 * 
	 * @param k
	 */
	public void setKeywords(Collection<String> k){
		// current set of authors assumed existing ; but creates it of called from ghost ref e.g.
		if(keywords==null){keywords=new HashSet<String>();}
		for(String s:k){keywords.add(s);}
	}
	
	/**
	 * Authors as string.
	 * @return
	 */
	public String getAuthorString(){
		try{
			String res="";
			for(String a:authors){res=res+","+a;}
			if(res.length()>0){res.substring(0, res.length()-1);}
			return res;
			// FIXME why could there be an exception here ? uninitialized authors ?
		}catch(Exception e){return "";}
	}

	/**
	 * Keywords as string.
	 * 
	 * @return
	 */
	public String getKeywordString(){
		try{
			String res="";
			for(String a:keywords){res=res+";"+a;}
			if(res.length()>0){res = res.substring(0, res.length()-1);}
			return res;
		}catch(Exception e){return "";}
	}


	/**
	 * propagates depth into the citation network
	 * FIXME not secure to reciprocal citations
	 * @param initialDepth
	 */
	public void setDepth(int initialDepth){
		depth = Math.max(depth,initialDepth);
		for(Reference c: citing){
			c.setDepth(depth-1);
		}
	}

	/**
	 * set and propagates horizontal depth into the citation network
	 * FIXME not secure to reciprocal citations
	 * @param origin
	 * @param depth
	 */
	public void setHorizontalDepth(String origin,int depth){
		if(horizontalDepth.containsKey(origin)){horizontalDepth.put(origin,new Integer(Math.min(depth,horizontalDepth.get(origin).intValue())));}
		else {horizontalDepth.put(origin,new Integer(depth));}
		for(Reference c: citing){
			c.setHorizontalDepth(origin,depth);
		}
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
		
		if(scholarID!=null&&scholarID!=""){
			return scholarID.hashCode();
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
			if(r.scholarID!=null&&r.scholarID!=""&&scholarID!=null&&scholarID!=""){
				return r.scholarID.equals(scholarID);
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
		return "Ref "+id+" - schID : "+scholarID+" - t : "+title+" - year : "+year+" - Cited by : "+citing.size()+" - authors : "+getAuthorString()+" - keywords : "+getKeywordString();
	}
	
}
