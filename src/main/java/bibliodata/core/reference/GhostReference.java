/**
 * 
 */
package bibliodata.core.reference;

/**
 * A Ghost Reference may be seen as an 'unverified' Reference, or of which the construction is unfinished.
 * Used to store ref infos without messing with hashconsing.
 * 
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class GhostReference extends Reference {
	
	//public GhostReference(){}
	
	/**
	 * 
	 * 
	 * @param t
	 */
	public GhostReference(String t){
		super(t);
	}
	
	public GhostReference(String t,String y){
		super(t);
		setYear(y);
	}
	
	public GhostReference(String i,String t,String r,String y){
		super(t);
		this.setId(i);
		this.setResume(r);
		if(y!=null){setYear(y);}else{setYear("");}
	}
	
	
	@Override
	public int hashCode(){
		return getTitle().title.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return(o instanceof GhostReference)&&(((Reference)o).getTitle().title.equals(getTitle().title));
	}
	
}
