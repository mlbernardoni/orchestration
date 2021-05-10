package R2s;

import java.io.BufferedReader;
import java.util.concurrent.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;


public class R2SendThread implements  Runnable {

  // public Thread t;
   
   private JSONObject myrow;
   private Semaphore mysemaphore;
   public int retcode;
   public String errorstring;


   
   R2SendThread(JSONObject row, Semaphore semaphore) {
	   
	   myrow = row;
	   mysemaphore = semaphore;
	   retcode = 500;
	   errorstring = "";
  }
   public void run() {
	   
	   String root_id = myrow.getString("root_service");
	   String parent_id = myrow.getString("parent_service");
	   String service_id = myrow.getString("service");
	    		  
	   String new_url = myrow.getString("service_url");
	   String new_param = myrow.getString("service_param");
	   
	   int mytries = myrow.getInt("tries");
	   int mytimeout = myrow.getInt("timeout");
	   int mytimeoutwait = myrow.getInt("timeoutwait");
	   
		  
		  while (mytries > 0)
		  {
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

				  StringBuffer resp = new StringBuffer();
				  
				  URL url = new URL(new_url);
				  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				  connection.setConnectTimeout(mytimeout);		// TIMEOUT - server took to long to even accept the request
				  connection.setReadTimeout(mytimeout);		// TIMEOUT - server accepted the request but taking too long, will through timeout exception
				  
				  // For a POST request
				  connection.setRequestMethod("POST");
				  connection.setRequestProperty("Content-Type", "application/json; utf-8");
				  connection.setDoOutput(true);

				  //System.out.println("R2Lib Sendevent Retries =  " + retries);
				  DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				  wr.writeBytes(newrequest.toString());
				  wr.flush();
				  wr.close();
				  retcode = connection.getResponseCode();
				  //System.out.println("R2Lib Ret Code: " + responseCode);
				  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				  String output;
				 
				  while ((output = in.readLine()) != null) 
				  {
					  resp.append(output);
				  }
				  in.close();
				  if (retcode == 200)
				  {			  
					  connection.disconnect();

					  mysemaphore.release();
					  return;
				  }
				  else
				  {
					  errorstring = (errorstring + " connection: " + resp + " param: " + newrequest.toString() + ";");
					  connection.disconnect();
					  //System.out.println("R2Lib Ret Code: " + responseCode);
					  //throw new IOException("IO Error sending to R2 ret code: " + responseCode);
					  mytries--;
					  if (mytries > 0) {
						  try 
						  {
							  TimeUnit.MILLISECONDS.sleep(mytimeoutwait);	// add a little wait, to see if root will end
						  }
						  catch (JSONException | InterruptedException ie) 
						  {
							  mysemaphore.release();
							  throw new IOException("InterruptedException " + ie.toString());
						  }						  
					  }
					  
				  }
			  }
			  catch (java.net.SocketTimeoutException e) {
				  errorstring = (errorstring + "Timeout: " + e.toString() + ";");
				  //System.out.println("R2Lib Timeout: " + e.toString());
				  //throw new IOException("R2Lib Timeout: " + e.toString());	// catch TIMEOUT here
				  mytries--;
				  if (mytries > 0) {
					  try 
					  {
						  TimeUnit.MILLISECONDS.sleep(mytimeoutwait);	// add a little wait, to see if root will end
					  }
					  catch (JSONException | InterruptedException ie) 
					  {
						  mysemaphore.release();
						  errorstring = errorstring + "InterruptedException" + ie.toString() + ";";
						  return;
					  }
				  }
			  }
			  catch (Exception e) 
			  { 
				  errorstring = (errorstring + "Exception: " + e.toString() + ";");
				  System.out.println("R2s Send Exception: " + e.toString());
				  mytries--;
				  if (mytries > 0) {
					  try 
					  {
						  TimeUnit.MILLISECONDS.sleep(mytimeoutwait);	// add a little wait, to see if root will end
					  }
					  catch (JSONException | InterruptedException ie) 
					  {
						  mysemaphore.release();
						  errorstring = errorstring + "InterruptedException" + ie.toString() + ";";
						  return;
					  }
				  }
			  }
		  }
		  
		  mysemaphore.release();
	      		  
    }
/*
   public void start () {
	   t = new Thread (this, "SendThread");
	   t.start ();
   }

*/
}