/**
 *
 */
package bibliodata.scholar;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLContext;

import bibliodata.Context;
import bibliodata.core.corpuses.Corpus;
import bibliodata.core.reference.Abstract;
import bibliodata.core.reference.Reference;
import bibliodata.core.reference.Title;

import bibliodata.database.mongo.MongoCommand;
import bibliodata.database.mongo.MongoConnection;
import bibliodata.database.mongo.MongoReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.commons.httpclient.util.URIUtil;

import bibliodata.utils.Log;
import bibliodata.utils.proxy.TorPoolManager;
import bibliodata.utils.proxy.TorThread;

/**
 * @author Raimbault Juste <br/> <a href="mailto:juste.raimbault@polytechnique.edu">juste.raimbault@polytechnique.edu</a>
 * TODO for refs with citing > 1000 use by year (see https://arxiv.org/abs/2004.14329)
 */
public class ScholarAPI {


	public static DefaultHttpClient client;
	public static HttpContext context;

	public static TorThread tor;




	/**
	 *
	 * TODO
	 *
	 *   - more robust archi for requests : any request (initial, or citations ?) must go through
	 *   scholarRequest function to ensureConnection ; request and ensureConnection being called only in scholarRequest
	 *
	 *
	 */



	/**
	 * Init a scholar client
	 *
	 * Independent from TorPool initialization ;
	 * TODO : clarify setup function ¡¡
	 *
	 *
	 */
	public static void init(){
		try{

			Log.stdout("Initializing scholar API...");

		    client = new DefaultHttpClient();

		    //context
		    context = new BasicHttpContext();
		    //add a cookie store to context
		    CookieStore cookieStore = new BasicCookieStore();
			context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		    //System.out.println(cookieStore.getCookies().size());

			//set timeout
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 10000);
			HttpConnectionParams.setSoTimeout(params, 10000);

			SSLContext sslContext = null;
			sslContext = SSLContext.getInstance("TLS")  ;
			sslContext.init(null,null,null);
			SSLSocketFactory sf = new SSLSocketFactory(sslContext,SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			Scheme sch = new Scheme("https", 443, sf);
			client.getConnectionManager().getSchemeRegistry().register(sch);

			 HttpGet httpGet = new HttpGet("http://scholar.google.com/scholar?q=transfer+theorem");
			 httpGet.setHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");
			 HttpResponse resp = client.execute(httpGet,context);

			 Element el = Jsoup.parse(resp.getEntity().getContent(),"UTF-8","");
			 try{Log.stdout("Accepted : "+(el.getElementsByClass("gs_r").size()>0));}catch(Exception e){e.printStackTrace();}

			 //System.out.println(el.html());
			 //System.out.println("Connected to scholar, persistent through cookies. ");
			 //for(int i=0;i<cookieStore.getCookies().size();i++){System.out.println(cookieStore.getCookies().get(0).toString());}

			 EntityUtils.consumeQuietly(resp.getEntity());

			}catch(Exception e){e.printStackTrace();}
	}




	/**
	 * @param references
	 */
	public static void fillIds(HashSet<Reference> references) {

		for(Reference r:references){
			try{
				if(r.getId()==null||r.getId()==""){
					Reference rr = getScholarRef(r);
					if(rr!=null){
						r.setId(rr.getId());
						Log.stdout("Retrieved ID for Ref "+r);
					}
				}
			}catch(Exception e){e.printStackTrace();}
		}

	}





	/**
	 * Get references from a scholar request - citations not filled for more flexibility.
	 *
	 *
	 * @param request : either title, keywords, or ID in citing case
	 * @param maxNumResponses number of results - physical limit of 990
	 * @param requestType "direct" or "cites"
	 * @return
	 */
	public static List<Reference> scholarRequest(String request, int maxNumResponses, String requestType){
		//HashSet<Reference> refs = new HashSet<Reference>();
		LinkedList<Reference> refs = new LinkedList<Reference>();

		String query = "";

		// encode query here ?
		try{request = URIUtil.encodePath(request);}catch(Exception e){}

		switch (requestType){
		   case "direct": query="scholar?q="+request;break;
		   case "exact" : query="scholar?as_q="+request;break;
		   //case "exact" : query="scholar?q=\""+request+"\"";break;
		   case "cites": query="scholar?cites="+request;break;
		}


		try{


		    addPage(refs,ensureConnection(query+"&lookup=0&start=0"),maxNumResponses);
			int resultsNumber = refs.size();

			 for(int l=10;l<maxNumResponses;l=l+10){
			     addPage(refs,ensureConnection(query+"&lookup=0&start="+l),maxNumResponses-resultsNumber);
			     if(refs.size()==resultsNumber){break;}
			     resultsNumber = refs.size();
			 }
		}catch(Exception e){e.printStackTrace();}

		Log.stdout("req : "+refs.size()+" results");

		return refs;
	}


	/**
	 * Get the ref constructed from scholar ; null if no result.
	 *
	 * @param title
	 * @return
	 */
	public static Reference getScholarRef(String title,String author,String year){

		// first need to format title (html tags eg)
		title = Jsoup.parse(title).text();

		Reference res = null;
		// go up to 5 refs in case of an unclustered ref (cf Roger Dion paper !)
		res=matchRef(title,author,year,scholarRequest(title.replace(" ", "+" ),5,"exact"));

		//try direct if no result
		if(res==null){
			res=matchRef(title,author,year,scholarRequest(title.replace(" ", "+" ),5,"direct"));
		}

		// try exact pattern with "title"
		if(res==null){
			res=matchRef(title,author,year,scholarRequest("\""+title.replace(" ", "+" )+"\"",5,"direct"));
		}

		return res;
	}

	/**
	 * Overload the method : call on title and concatenated authors.
	 *
	 * @param ref
	 * @return
	 */
	public static Reference getScholarRef(Reference ref){
		String authors = "";for(String a:ref.getAuthors()){authors = authors+" "+a;}
		String year = ref.getYear();
		if(year.contains("-")){year = year.split("-")[0];}
		Reference res = getScholarRef(ref.getTitle().title,authors,year);
		if(res==null){
			ref.getAttributes().put("failed_req", "1");
		}else{
			ref.getAttributes().put("failed_req", "0");
		}
		return res;
	}


	/**
	 * Match a ref to a set, looking at title and if refs have sch ids
	 *
	 * @param refs
	 * @return
	 */
	public static Reference matchRef(String title,String author,String year,List<Reference> refs){
		Reference res = null;
		for(Reference nr:refs){
			Log.stdout(nr.getYear()+"  --  "+year);
			String t1 = StringUtils.lowerCase(nr.getTitle().title).replaceAll("[^\\p{L}\\p{Nd}]+", "");
			String t2 = StringUtils.lowerCase(title).replaceAll("[^\\p{L}\\p{Nd}]+", "");
			Log.stdout("      "+t1);
			Log.stdout("      "+t2);
			if(StringUtils.getLevenshteinDistance(t1,t2)<3&&nr.getId()!=""&&year.compareTo(nr.getYear())==0){
			   res=nr;
			}
		};
		return res;
	}


	/*
	public static void fillIdAndCitingRefs(Corpus corpus){
		fillIdAndCitingRefs(corpus,"");
	}
	*/

	/**
	 * Queries and constructs citing refs for a set of refs, and fills scholar id.
	 *
	 * Alters refs in place.
	 *
	 * @param corpus
	 */
	public static void fillIdAndCitingRefs(Corpus corpus,String consolidationDatabase,boolean consolidationOnly){
		try{
			int totalRefs = corpus.references.size();int p=0;
			for(Reference r:corpus.references){
				Log.stdout("Getting cit for ref "+r.toString());

				// FIXME first condition is not correct, could refill refs at a more recent date
				// FIXME why 1 and not 0 ?
				if(r.getCiting().size()>1||r.isCitingFilled()){
					Log.stdout("Citing refs already filled : "+r.getCiting().size()+" refs");
				}
				else{
					try{
						// first get scholar ID

						/**
						 * TODO : some refs are only in VO (eg french -> must have both titles and try request on list of titles)
						 *
						 * TODO : write a generic distance function, binary, taking title and authors, title being compared on
						 * non special characters (-> levenstein on non special ?)
						 * AND with good language title
						 *
						 */

						Reference rr = Reference.empty;
						if (consolidationDatabase.length()>0&r.hasId()){
							MongoConnection.switchMongo(consolidationDatabase);
							rr = MongoReference.getReference(r.getId());
							Log.stdout("Ref "+rr.getId()+" got from mongo");
						}

						// in the case of conso only (no scholar), do not run the rest - the ref has been constructed with
						// citing if it had been collected before
						if(!consolidationOnly) {

							if (rr.isEmpty()) {
								// FIXME require ref details only if no ID -> should be an option to get other available fields
								// FIXME also we should not have null pointers
								if (!r.hasId()) {
									rr = getScholarRef(r);
								} else {
									rr = r;
								}
							}


							// collect citations if not empty and if not already citing filled
							if (!rr.isEmpty() & !rr.isCitingFilled()) {
								Log.stdout("ID : " + rr.getId());
								//r.scholarID=rr.scholarID;//no need as rr and r should be same pointer ?
								// FIXME with unique ids, this never happens
								//if(!rr.equals(r)){Reference.references.remove(r);} //contradiction with hashconsing ? - DIRTY
								if (!rr.equals(r)) {
									Reference.removeReference(r);
								}
								r = rr;

								//  limit of max cit number -> global parameter - shouldnt be larger than 1000 (failure in collection then)
								List<Reference> citing = scholarRequest(r.getId(), Context.getScholarMaxRequests(), "cites");
								//for(Reference c:citing){r.setCiting(c);}
								r.setCiting(citing);
								r.setCitingFilled(true);
								r.setTimestamp(Log.currentTimestamp());

								// update depth of citing refs
								for (Reference citingRef : r.getCiting()) {
									citingRef.setDepth(Math.max(r.getDepth() - 1, citingRef.getDepth()));
								}
							}

						}
						// FIXME generic inheritance of parents properties here ? (cf horizontalDepth)

						Log.stdout("Citing refs : "+r.getCiting().size());

					}catch(Exception e){e.printStackTrace();}
				}

				// does not make sense in Mongo mode (ref by ref) -> removed
				//Log.purpose("progress","Corpus "+corpus.name+" : citing refs : "+(100.0 * (1.0*p) / (1.0*totalRefs))+ " % ; ref "+r.toString());p++;

			}
		}catch(Exception e){e.printStackTrace();}
	}
	


	/**
	 * Switch TOR port to ensure scholar connection (google blocking).
	 *
	 * @param request
	 * @return
	 */
	private static Document ensureConnection(String request) {
		Document dom = new Document("<html><head></head><body></body></html>");
		Log.stdout("Request : "+request);
		try{dom=request("scholar.google.com",request);}
		catch(Exception e){e.printStackTrace();}
		//Log.stdout(dom.html());
		try{Log.stdout(dom.getElementsByClass("gs_rt").first().text());}catch(Exception e){}
		try{Log.stdout(dom.getElementsByClass("gs_alrt").first().text());}catch(Exception e){}

		try{
			//if(dom.getElementById("gs_res_bdy")==null){
				//System.out.println(dom.html());
				//while(dom==null||dom.getElementById("gs_res_bdy")==null){
				while(dom==null||dom.getElementById("gs_res_ccl")==null){
					// swith TOR port
					Log.stdout("Current IP failed ; switching currentTorThread.");

				    // store for systematic stats of blocked adress (may have patterns in blocking policy)
					//  how to get ref retrieved by this ip only ? : store last increase
					if(Context.getLogips()) MongoCommand.logIP(TorPoolManager.currentIP,false,0);

					// FIXME port exclusivity as a global parameter ?
				    TorPoolManager.switchPort(true);

					// reinit scholar API
					init();
					//update the request
					dom = request("scholar.google.com",request);
					try{Log.stdout(dom.getElementsByClass("gs_rt").first().text());}catch(Exception e){}
					try{Log.stdout(dom.getElementsByClass("gs_alrt").first().text());}catch(Exception e){}
				}
			//}
			// at this stage the ip is accepted
			// tricky to get exact number of refs ? -> put counter with ip in base
			// a supplementary dom corresponds roughly to 10 more (potential) references
			if(Context.getLogips()) MongoCommand.logIP(TorPoolManager.currentIP,true,10);

		}catch(Exception e){e.printStackTrace();}
		return dom;
	}


	/**
	 * Local function parsing a scholar response.
	 *
	 * @param refs
	 * @param dom
	 * @param remResponses
	 */
	private static void addPage(List<Reference> refs,Document dom,int remResponses){
		//,String origin,int pageHorizDepth // not needed to be done at this depth as the retrived list is ordered !
		int resultsNumber = 0;
		Elements e = dom.getElementsByClass("gs_ri");
		for(Element r:e){
	    	if(resultsNumber<remResponses){
	    		String id = getCluster(r);
	    		// FIXME we make the choice here to add only if cluster is identified (i.e. different versions or cited in scholar) => could be added as an option (but requires unique id creation)
	    		// plus could get the users = authors here !
				// TODO add author collection (-> cocitation networks)
				// for exact citation/all authors/bibtex -> needs js emulation (out of question for now for perf reasons)
				if(id!=null&&id.length()>0){
					Reference toadd = Reference.construct(id, getTitle(r), new Abstract(), getYear(r));
					//toadd.horizontalDepth.put(origin,new Integer(pageHorizDepth+i));
	    		  	refs.add(toadd);
	    		  	resultsNumber++;
	    		}
	    	}
	    }
	}

	/**
	 * Get cluster from an element
	 *
	 * @param e
	 */
	private static String getCluster(Element e){
		String cluster = "";
		try{
		   cluster = e.getElementsByAttributeValueContaining("href", "/scholar?cites=").first().attr("href").split("scholar?")[1].split("cites=")[1].split("&")[0];
		}catch(NullPointerException nu){

			//null pointer -> not cited, try "versions" link to get cluster
			try{cluster = e.getElementsByAttributeValueContaining("href", "/scholar?cluster=").first().attr("href").split("scholar?")[1].split("cluster=")[1].split("&")[0];}
			catch(Exception nu2){}
		}
		return cluster;
	}

	/**
	 * Get title given the element.
	 *
	 * @param e
	 * @return
	 */
	private static Title getTitle(Element e){
		try{
		  return new Title(e.getElementsByClass("gs_rt").text().replaceAll("\\[(.*?)\\]",""));
		}catch(Exception ex){ex.printStackTrace();return Title.EMPTY;}
	}


	/**
	 * Get year
	 *
	 * @param e
	 * @return
	 */
	private static String getYear(Element e){
		try{
			String t = e.getElementsByClass("gs_a").first().text();
			String y = "";
			for(int i=0;i<t.length()-4;i++){
				if(t.substring(i, i+4).matches("\\d\\d\\d\\d")){y=t.substring(i, i+4);};
			}
			return y;
		}catch(Exception ex){ex.printStackTrace();return "";}
	}


	/**
	 * Simple HTTP Get request to host, url.
	 *
	 * @param host
	 * @param url
	 * @return org.jsoup.nodes.Document dom
	 */
	public static Document request(String host,String url){
		Document res = null;
		try {

			//String encodedURL = URIUtil.encodeWithinPath("http://"+host+"/"+url);
			// needs to be done before.

			String encodedURL = "http://"+host+"/"+url;

			Log.stdout("Request : "+encodedURL);

			HttpGet httpGet = new HttpGet(encodedURL);
			httpGet.setHeader("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");
			// FIXME should ne able to set the timeout here
			HttpResponse response = client.execute(httpGet);
		    try {
		    	//res= Jsoup.parse(response.getEntity().getContent(),"UTF-8","");
		    	res= Jsoup.parse(EntityUtils.toString(response.getEntity(),"UTF-8"));
				//try{Log.stdout("Results : "+(res.getElementsByClass("gs_r").size()>0));}catch(Exception e){}
		    	EntityUtils.consume(response.getEntity());
		    }catch(Exception e){e.printStackTrace();}
		} catch(Exception e){e.printStackTrace();}
		return res;
	}



	public static void main(String[] args){
		init();
	}





}
