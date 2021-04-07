package r2fileapp;

import java.io.BufferedReader;
import java.util.concurrent.*;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;


public class R2SendThread extends Thread {

   private Thread t;
   private String root_id;
   private String parent_id;
   private String service_id;
   private String new_url;
   private String new_param;
   private Semaphore mysemaphore;
   
   R2SendThread( String rootid, String parentid, String serviceid, String newurl, String newparam, Semaphore semaphore) {
	   root_id = rootid;
	   parent_id = parentid;
	   service_id = serviceid;
	   new_url = newurl;
	   new_param = newparam;
	   mysemaphore = semaphore;
	   
   }
   public void run() {
		  StringBuffer resp = new StringBuffer();
	      //System.out.println("Thread Starting: ");
	      //System.out.println(service_id);
		  try 
		  {

			  mysemaphore.acquire();
		      JSONObject newservice = new JSONObject();
			  newservice.put("root_service", root_id);
			  newservice.put("parent_service", parent_id);
			  newservice.put("service", service_id);
			  newservice.put("service_param", new_param);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("r2_msg", newservice);
		      //System.out.println(newservice.toString());
			  
			  URL url = new URL(new_url);
			  HttpURLConnection connection = R2_Utilities.setupPostRequest(url); 
			  connection.setRequestProperty("Connection", "close");

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
			  connection.disconnect();
		  }
		  catch (Exception e) 
		  { 
			  /*report an error*/ 
			  // crash and burn
			  //throw new IOException("Error sending from Rtoos");
		  }
	      //System.out.println("Thread Ending: ");
	      //System.out.println(service_id);
	      mysemaphore.release();
    }

   public void start () {
	         t = new Thread (this, "SendThread");
	         t.start ();
   }
}

