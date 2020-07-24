/**
 * 
 */
package bibliodata.mendeley;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import bibliodata.core.AlgorithmicSystematicReview;
import bibliodata.utils.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import bibliodata.utils.Connexion;
import bibliodata.utils.Log;
import bibliodata.core.reference.Abstract;
import bibliodata.core.reference.GhostReference;
import bibliodata.core.reference.Reference;
import bibliodata.core.reference.Title;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 *
 */
public class MendeleyAPI{


	/**
	 * Mendeley api
	 */
	public static String mendeleyAppId;

	/**
	 * Mendeley secret
	 */
	public static String mendeleyAppSecret;

	/**
	 * Http client
	 */
	public static DefaultHttpClient client;
	
	/**
	 * Http context
	 */
	public static HttpContext context;
	
	/**
	 * Current access token to the api.
	 */
	public static String accessToken;
	
	/**
	 * When (in system ms) the current access token was created.
	 */
	public static long accessTokenBirth;
	
	/**
	 * If api setup has been performed.
	 */
	public static boolean isSetup = false;
	
	
	/**
	 * Initialize API requests, by setting client and context.
	 */
	@SuppressWarnings("resource")
	public static void setupAPI(String mendeleyconf){
		try{
		    Log.stdout("Setting up Mendeley API...");

			HashMap<String,String> confsMap = CSVReader.readMap(mendeleyconf, ":","");

			// mendeley
			if(confsMap.containsKey("appID")){mendeleyAppId = confsMap.get("appID");}
			if(confsMap.containsKey("appSecret")){mendeleyAppSecret=confsMap.get("appSecret");}


			client = new DefaultHttpClient();
			client.getCredentialsProvider().setCredentials(AuthScope.ANY,new UsernamePasswordCredentials(mendeleyAppId,mendeleyAppSecret));

			//context
			context = new BasicHttpContext();
		
			// empty access token
			accessToken = "";
		
			isSetup=true;
		
		}catch(Exception e){e.printStackTrace();}
	}
	
	
	/**
	 * Get access token from auth api.
	 *   Token is renewed only when needed.
	 * 
	 * @param renewToken force token renewal.
	 * 
	 * @return
	 */
	public static String getAccessToken(boolean renewToken){
		try{
			// auth query if renewal is forced
			if(renewToken||(System.currentTimeMillis()-accessTokenBirth)>3000000||accessToken==""){
				Log.output("Getting access token to Mendeley api -- previous token age : "+(System.currentTimeMillis()-accessTokenBirth));
				
				//auth is a basic post request
				HashMap<String,String> header = new HashMap<String,String>();
				header.put("Content-Type", "application/x-www-form-urlencoded");

				// url
				String url = "https://api.mendeley.com/oauth/token";

				//post data
				HashMap<String,String> data = new HashMap<String,String>();
				data.put("grant_type", "client_credentials");
				data.put("scope", "all");

				HttpResponse res = Connexion.post(url,header,data,client,context);

				//String resp = (new BufferedReader(new InputStreamReader(res.getEntity().getContent())).readLine());
				// do not need to convert to string, as json reader can directly read string

				//parse json string
				JsonReader jsonReader = Json.createReader(res.getEntity().getContent());
				JsonObject object = jsonReader.readObject();
				jsonReader.close();

				// return access token
				// Q : use refresh token ? not really necessary if renewed each hour.

				Log.stdout("API Response: "+object.toString());

				String token = object.getString("access_token");
				Log.output("Token : "+token);
				
				// set current access token and birth date
				accessToken = token;
				accessTokenBirth = System.currentTimeMillis();
						
				return token;
			}else{
				// return current access token
				return accessToken;
			}
		}
		catch (Exception e) {e.printStackTrace();return "";}
	}
	
	
	
	/**
	 * Mendeley catalog request
	 * 
	 * @param query
	 * @param numResponse
	 * @param ghostRefs
	 * @return
	 */
	public static HashSet<Reference> catalogRequest(String query,int numResponse,boolean ghostRefs){
		
		Log.stdout("Mendeley query : "+query);
		
		HashSet<Reference> refs = new HashSet<Reference>();
		
		try{
			
			//get an access token
			String token = getAccessToken(false);
			
			// request
			JsonArray entries = (JsonArray) rawRequest(query,numResponse,token);
			
			// check if result, if not renew the access token and redo the request
			if(entries.size()==0){token = getAccessToken(true);entries = (JsonArray)rawRequest(query,numResponse,token);}

			//Log.stdout("API result: "+entries.toString());
			Log.stdout("API result size: "+entries.size());

			//iterate on the json object
			for(int i=0;i<entries.size();i++){
				JsonObject entry = entries.getJsonObject(i);
				
				//System.out.println(i+" : "+entry);//DEBUG
				//System.out.println(Integer.toString(entry.getInt("year")));

				String mendeleyId = "";
				if(entry.containsKey("id")) {mendeleyId = entry.getString("id");}
				String title = "";
				if(entry.containsKey("title")) {title = entry.getString("title");}
				String abs = "";
				if(entry.containsKey("abstract")) {abs = entry.getString("abstract");}
				String year = "";
				if(entry.containsKey("year")) {year = Integer.toString(entry.getInt("year"));}

				// add reference using construct -- no scholar ID
				Reference newref = null;
				if(ghostRefs){
					// ghost ref with fields id, title, abstract, year
					newref = new GhostReference(mendeleyId,title,abs,year);
				}else{
					newref = Reference.construct("", new Title(title), new Abstract(abs),year);
					newref.setSecondaryId(mendeleyId);
				}
				// in any case set authors and keywords
				newref.setAuthors(getAuthors(entry));
				newref.setKeywords(getKeywords(entry));

				// doi
				if (entry.containsKey("identifiers")){
					JsonObject ids = entry.getJsonObject("identifiers");
					if (ids.containsKey("doi")){
						newref.setAttribute("doi",ids.getString("doi"));
					}
				}

				// source
				if (entry.containsKey("source")){
					newref.setAttribute("source",entry.getString("source"));
				}

				// type
				if (entry.containsKey("type")){
					newref.setAttribute("type",entry.getString("type"));
				}

				refs.add(newref);
			}		
			return refs;
			
		}catch(Exception e){e.printStackTrace();return refs;}
	}
	
	/**
	 * Get authors set from json array.
	 */
	private static LinkedList<String> getAuthors(JsonObject entry){
		JsonArray authors = entry.getJsonArray("authors");
		LinkedList<String> res = new LinkedList<String>();
		//System.out.println(authors);
		if(authors!=null){
			for(int i=0;i<authors.size();i++){
				JsonObject author = authors.getJsonObject(i);
				String firstname = "";try{firstname = author.getString("first_name");}catch(Exception e){
					//Log.stdout("no author first name");
				}
				String lastname = "";try{lastname = author.getString("last_name");}catch(Exception e){Log.stdout("no author last name");}
				res.add(firstname+" "+lastname);
			}
		}
		return res;
	}
	
	/**
	 * Get keywords.
	 * 
	 * @param entry
	 * @return
	 */
	private static LinkedList<String> getKeywords(JsonObject entry){
		JsonArray keywords = entry.getJsonArray("keywords");
		LinkedList<String> res = new LinkedList<String>();
		if(keywords!=null){for(int i=0;i<keywords.size();i++){res.add(keywords.getString(i));}}
		return res;
	}
	
	
	
	
	/**
	 * Wraps a raw api request, given an access token.
	 * 
	 * @param query query string
	 * @param numResponse number of responses
	 * @param token api token
	 * @return json result
	 */
	private static JsonStructure rawRequest(String query,int numResponse,String token){
		HttpResponse res=null;
		try{
			//simple get request
			String url = "https://api.mendeley.com/search/catalog?query="+query+"&limit="+(Integer.toString(numResponse));
			HashMap<String,String> header = new HashMap<>();
			header.put("Accept", "application/vnd.mendeley-document.1+json");	
			header.put("Authorization", "Bearer "+token);
			res = Connexion.get(url,header,client,context);

			//rq : catalog request limited to 100 responses
			// Check headers to see if next page available ?
			//for(Header h:res.getAllHeaders()){System.out.println(h.toString());}

			JsonReader jsonReader = Json.createReader(res.getEntity().getContent());			
			
			JsonArray entries = jsonReader.readArray();
			jsonReader.close();

			EntityUtils.consumeQuietly(res.getEntity());
			
			return entries;
			
		}catch(JsonException je){
			// if json exception, object was read instead of array, returns empty array.
			EntityUtils.consumeQuietly(res.getEntity());
			return Json.createArrayBuilder().build();
		}catch(Exception e){
			e.printStackTrace();
			EntityUtils.consumeQuietly(res.getEntity());
			// return an empty array
			return Json.createArrayBuilder().build();
		}
	}
	
	
	
	/**
	 * Get reference from title, same way than scholar api.
	 *   scholarID is requested here for performance, as hashcode for Reference without scholarID is in O(Nrefs)
	 * 
	 * @param title title
	 * @return matched ref (by title) or null if no result
	 */
	public static Reference getReference(String title,String scholarID){
		// get potential references as ghost references
		//HashSet<Reference> potentialRefs = MendeleyAPI.catalogRequest(title.replaceAll(" ","+").replaceAll("\\{", "").replaceAll("\\}", ""),10,true);
		String reqstring = title.replaceAll(" ","+").replaceAll("[^\\p{L}\\p{Nd}\\+]+","");
		HashSet<Reference> potentialRefs = MendeleyAPI.catalogRequest(reqstring,100,true);
		
		
		// match it
		Reference res = matchRef(title,potentialRefs);
		
		if(res!=null){
			// unghost it -> construct static method with GhostReference single argument.
			res = Reference.materialize((GhostReference)res,scholarID);
		}
		
		return res;
	}
	
	/**
	 * Same criteria than for scholar ref to match a ref.
	 *  - do not use author and year, match on title only (too much jitter on year? - could add small threshold?)
	 * @param title original title
	 * @param refs refs to match
	 * @return best matching res (arbitrary threshold of 3 for Levenstein distance)
	 */
	 private static Reference matchRef(String title,HashSet<Reference> refs){
		Reference res = null;
		for(Reference nr:refs){
			//Log.stdout(nr.getYear()+"  --  "+year);
			String t1 = StringUtils.lowerCase(nr.getTitle().title).replaceAll("[^\\p{L}\\p{Nd}]+", "");
			String t2 = StringUtils.lowerCase(title).replaceAll("[^\\p{L}\\p{Nd}]+", "");
			//Log.stdout(nr.toString());
			Log.stdout("      "+t1);
			Log.stdout("      "+t2);
			if(StringUtils.getLevenshteinDistance(t1,t2)<3){//&&year.compareTo(nr.year)==0){
			   res=nr;
			}
		}
		Log.stdout("Matched: "+res);
		return res;
	}
	
	
	
	/*
	 * Get abstracts for the given set of References
	 *  -> done in AbstractSetRetriever
	 */
	/*public static void getAbstracts(HashSet<Reference> corpus){
		
	}*/
	
	
	
	
	
	
	/**
	 * TEST
	 */
	public static void main(String[] args) throws Exception {

		setupAPI("conf/mendeley");
		
		// Test token request
		//System.out.println(rawRequest("transfer+theorem",10,getAccessToken(true)));
		
		
		// test catalog request rate
		// HASHCODE SHITS OVER  -> OK pb in testing null or empty schID
		for(int k=0;k<100;k++){
			System.out.println();
			System.out.println("-- "+k+" --");
			//HashSet<Reference> c = catalogRequest("transfer+theorem",3,false);
			Reference r = getReference("Modéliser les pratiques pastorales d’altitude dans la longue durée","7976888532897584518");
			//for(Reference r:c){System.out.println(r);}
			System.out.println(r);
			
			Thread.sleep(100);
		}
		
		
	}
	

}
