package bibliodata.utils.proxy;

import bibliodata.utils.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * same archi as torpool ?
 *  https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html
 *
 * public hhtp proxy list
 *   https://www.proxy-list.download/api/v1/get?type=http
 *
 * https://proxy-daily.com/
 *
 */
public class ProxyManager {

    private static final String ipurl = "http://ipecho.net/plain";

    // FIXME do not hardcode
    private static final String proxylisturl = "https://www.proxy-list.download/api/v1/get?type=http";

    public static final LinkedList<String> httpproxies=new LinkedList<String>();

    public static void initHttpProxies(){


        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("curl "+proxylisturl).getInputStream()));
            //BufferedReader r = new BufferedReader(new InputStreamReader(new URL(proxylisturl).openConnection().getInputStream()));
            String currentLine = r.readLine();
            while (currentLine != null) {
                //System.out.println(currentLine);
                httpproxies.add(currentLine);
                currentLine = r.readLine();
            }
            System.out.println(httpproxies.size());
        }catch(Exception e){e.printStackTrace();}

    }


    public static void switchHttpProxy(){

        // clear possible socks
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");

        // set http proxy
        String previousHost = System.getProperty("http.proxyHost","");
        String previousPort = System.getProperty("http.proxyPort","");
        if (previousHost.length()>0&&previousPort.length()>0) httpproxies.addLast(previousHost+":"+previousPort);
        String[] newproxy = httpproxies.removeFirst().split(":");
        System.out.println(newproxy[0]+":"+newproxy[1]);
        System.setProperty("http.proxyHost", newproxy[0]);
        System.setProperty("http.proxyPort", newproxy[1]);

        try {
            System.out.println("Http proxy IP: "+new BufferedReader(new InputStreamReader(new URL(ipurl).openConnection().getInputStream())).readLine());
        }catch(Exception e){}

    }


    public static void main(String[] args){
        initHttpProxies();
        for(int k =0;k<20;k++){
            switchHttpProxy();
        }
    }


}
