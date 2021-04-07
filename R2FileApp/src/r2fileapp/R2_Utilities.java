package r2fileapp;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class R2_Utilities {
	
	public static HashMap<String, ArrayList<String>> initializeList(HashMap<String, ArrayList<String>> list, String key, String newItem) {
		if (list.get(key) == null) {
		    list.put(key, new ArrayList<String>());
 		}
	    list.get(key).add(newItem);
	    return list;
	}
	
	public static HttpURLConnection setupPostRequest(URL url) throws Exception {
		try {
		  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		  
		 // For POST request
		  connection.setRequestMethod("POST");
		  connection.setRequestProperty("Content-Type", "application/json; utf-8");
		  connection.setDoOutput(true);
		  return connection; 
		  
		} catch (MalformedURLException e) {
		    System.out.println("Malformed URL Exception"); 
		    e.printStackTrace();
		    return null; 
		} catch (Exception e) {
			e.printStackTrace();
			return null; 
		}
	}

}
