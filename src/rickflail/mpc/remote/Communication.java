package rickflail.mpc.remote;

import java.util.HashMap;
import java.util.regex.*;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class Communication {
	
	String server;
	String port;
	
	HttpParams httpParams;
	HttpClient httpClient;

	public Communication(String srv, String prt) {
		server = srv;
		port = prt;
		
		httpParams = new BasicHttpParams();
		
		HttpConnectionParams.setConnectionTimeout(httpParams, 900);
		HttpConnectionParams.setSoTimeout(httpParams, 900);
		
		httpClient = new DefaultHttpClient(httpParams);
	}
	
	public String sendCommand(String command) {
		String html = getResponse("http://"+server+":"+port+"/command.html?wm_command="+command);
		return html;
	}
	
	public HashMap<String, String> getVariables() {
		HashMap<String, String> vars = new HashMap<String, String>();
		
		String html = getResponse("http://"+server+":"+port+"/variables.html");
		
		Pattern regex = Pattern.compile("<p id=\"(.+)\">(.+)</p>");
		Matcher match = regex.matcher(html);
		
		while (match.find()) {
			vars.put(match.group(1), match.group(2));
		}
		
		if (vars.containsKey("filepath")) {
			regex = Pattern.compile("[^\\\\]+$");
			match = regex.matcher(vars.get("filepath"));
			if (match.find()) {
				vars.put("filename", match.group(0));
			}
		} else {
			regex = Pattern.compile("error: (.*)");
			match = regex.matcher(html);
			if (match.find()) {
				vars.put("error", match.group(1));
			} else {
				vars.put("error", "No content or invalid content returned");
			}
		}
		
		return vars;
	}
	
	private String getResponse(String url) {
		try {
            HttpGet httpGet = new HttpGet(url);

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody;
            responseBody = httpClient.execute(httpGet, responseHandler);
            
            return responseBody;
		} catch (Exception e) {
			return "error: No Connection";
		}
	}

}
