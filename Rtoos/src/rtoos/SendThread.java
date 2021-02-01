package rtoos;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONObject;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

public class SendThread extends Thread {

   private Thread t;
   private String root_id;
   private String parent_id;
   private String event_id;
   private String new_url;
   private String new_param;
   
   SendThread( String rootid, String parentid, String eventid, String newurl, String newparam) {
	   root_id = rootid;
	   parent_id = parentid;
	   event_id = eventid;
	   new_url = newurl;
	   new_param = newparam;
	   
   }
   public void run() {
		  StringBuffer resp = new StringBuffer();
		  try 
		  {

		      JSONObject newservice = new JSONObject();
			  newservice.put("type", "Event");
			  newservice.put("root_service", root_id);
			  newservice.put("parent_service", parent_id);
			  newservice.put("service", event_id);
			  newservice.put("service_param", new_param);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
		      //System.out.println(newservice.toString());
			  
			  URL url = new URL(new_url);
			  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			  // For a PUT request
			  connection.setRequestMethod("POST");
			  connection.setRequestProperty("Content-Type", "application/json; utf-8");
			  connection.setDoOutput(true);
			  DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			  wr.writeBytes(newrequest.toString());
			  wr.flush();
			  wr.close();
			  
			  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			  String output;
			 
			  while ((output = in.readLine()) != null) 
			  {
				  resp.append(output);
			  }
			  in.close();
		  }
		  catch (Exception e) 
		  { 
			  /*report an error*/ 
			  // crash and burn
			  //throw new IOException("Error sending from Rtoos");
		  }
    }

   public void start () {
	         t = new Thread (this, "SendThread");
	         t.start ();
   }
}

