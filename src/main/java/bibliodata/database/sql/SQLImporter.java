package bibliodata.database.sql;

import java.sql.ResultSet;
import java.util.HashSet;

import bibliodata.utils.Log;
import bibliodata.utils.proxy.TorPoolManager;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.corpuses.DefaultCorpus;
import bibliodata.core.reference.Abstract;
import bibliodata.core.reference.Reference;
import bibliodata.core.reference.Title;

/**
 *
 * Import corpus from sql
 *
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class SQLImporter {

	/**
	 *
	 * imports a corpus from simple database.
	 * 	 * marks principal refs with "principal" attribute
	 *
	 * @param database db name
	 * @param principalTableName principal table name
	 * @param secondaryTableName secondary table name (revues.org specification) ! may be deprecated now ?
	 * @param citationTableName citation links table
	 * @param numRefs number of references to get
	 * @param reconnectTorPool should the torpool be reconnected after the import (local ports conflict)
	 * @return imported corpus
	 */
	public static Corpus sqlImport(String database,String principalTableName,String secondaryTableName,String citationTableName,int numRefs,boolean reconnectTorPool){
		HashSet<Reference> refs = new HashSet<>();
		
		try{
			SQLConnection.setupSQL(database);

			// primary refs
			String primaryQuery = "SELECT * FROM "+principalTableName;
			if(numRefs!=-1){primaryQuery=primaryQuery+" LIMIT "+numRefs;}
			primaryQuery=primaryQuery+";";
			ResultSet resprim = SQLConnection.executeQuery(primaryQuery);
			//int primRefs = 0;
			while(resprim.next()){
				Reference r = Reference.construct(resprim.getString(1),new Title(resprim.getString(2)),new Abstract(),resprim.getString(3));
				refs.add(r);
				Log.stdout(r.toString());
				//primRefs++;
			}
			//set prim attribute
			for(Reference r:refs){r.setAttribute("primary", "1");}
			
			//secondary refs
			
			ResultSet ressec = SQLConnection.executeQuery("SELECT * FROM "+secondaryTableName+";");		
			while(ressec.next()){
				Reference r = Reference.construct(ressec.getString(1),new Title(ressec.getString(2)),new Abstract(),ressec.getString(3));
				refs.add(r);
				Log.stdout(r.toString());
			}
			
			// add citations -> refs already constructed, construct method gives refs
			ResultSet rescit = SQLConnection.executeQuery("SELECT * FROM "+citationTableName+";");	
			while(rescit.next()){
				Reference citing = Reference.construct(rescit.getString(1));
				Reference cited = Reference.construct(rescit.getString(2));
				Log.stdout(citing.getId() + " - " + cited.getId());
				cited.setCiting(citing);
				citing.getBiblio().cited.add(cited);
			}
			
			if(reconnectTorPool){TorPoolManager.setupTorPoolConnexion(true);}
		}catch(Exception e){e.printStackTrace();}

		return new DefaultCorpus(refs);
	}
	
	
	public static Corpus sqlImportPrimary(String database,String table,String status,int numRefs,boolean reconnectTorPool){
		HashSet<Reference> refs = new HashSet<>();
		try{
			SQLConnection.setupSQL(database);
			String query = "SELECT "+table+".id,title,year,status FROM "+table+" JOIN status ON status.id=cybergeo.id";
			if(status.length()>0){query=query+" WHERE status="+status;}
			if(numRefs != -1){query=query+" LIMIT "+numRefs;}
			query = query + ";";
			ResultSet resprim = SQLConnection.executeQuery(query);		
			while(resprim.next()){refs.add(Reference.construct(resprim.getString(1),new Title(resprim.getString(2)),new Abstract(),resprim.getString(3)));}
			if(reconnectTorPool){TorPoolManager.setupTorPoolConnexion(true);}
		}catch(Exception e){e.printStackTrace();}
		
		return new DefaultCorpus(refs) ;
	}
	
	
	public static HashSet<String> sqlSingleColumn(String database,String table, String column,boolean reconnectTorPool){
		HashSet<String> res = new HashSet<>();
		try{
			SQLConnection.setupSQL(database);
			String query = "SELECT "+column+" FROM "+table+";";
			ResultSet resq = SQLConnection.executeQuery(query);		
			while(resq.next()){res.add(resq.getString(1));}
			if(reconnectTorPool){TorPoolManager.setupTorPoolConnexion(true);}
		}catch(Exception e){e.printStackTrace();}
		
		return res;
	}
	
	
	
	
	
}
