package r2fileapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import com.datastax.driver.core.utils.UUIDs;


//import java.util.UUID;

public class R2_Lib {
	
	private JSONArray r2regarray = new JSONArray();
	private JSONObject r2json = null;
	private int R2_TIMEOUT = 600000;
	private int R2_TIMEOUT_WAIT = 3000;
	private int R2_RETRIES = 3;
	private String R2_URL = "";
	
	//
	// constructor - can only do "root" operation
	//
	R2_Lib()
	{
		R2_Init();
	}
	//
	// constructor - required to do anything other than root operation
	//
	R2_Lib(String jb) throws IOException
	{
		try {
			JSONObject r2_json =  new JSONObject(jb);
			r2json = (JSONObject)r2_json.get("r2_msg");
			R2_Init();
		}
	    catch (JSONException  e) 
	    {
	    	throw new IOException(jb + e.toString());
	    }
	}
	private void R2_Init()
	{
		// add init from file here
		R2_URL = "http://localhost:8080/R2FileApp/R2.html";
	}
	
	// 
	// retry and timeout public functions
	//
	public void R2_SetTimeout(int newtimeout) 
	{
		R2_TIMEOUT = newtimeout;
	}
	public void R2_SetTimeoutWait(int newwait) 
	{
		R2_TIMEOUT_WAIT = newwait;
	}
	public void R2_SetRetries(int newretries) 
	{
		R2_RETRIES = newretries;
	}

	
    // //////////////////////////////////////////////////////
	//
	// returns an id
	//
    // //////////////////////////////////////////////////////
	public String R2_GetID() 
	{
		return UUIDs.random().toString();
	}


    // //////////////////////////////////////////////////////
	//
	// helper functions to return message data
	//
    // //////////////////////////////////////////////////////
	public String R2_GetParam()  throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		return r2json.getString("service_param");
	}
	public String R2_GetRootID()  throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		return r2json.getString("root_service");
	}
	public String R2_GetParentID()  throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		return r2json.getString("parent_service");
	}
	public String R2_GetServiceID() throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		return r2json.getString("service");
	}


    // //////////////////////////////////////////////////////
	//
	// R2_Root
	//
    // //////////////////////////////////////////////////////
	public String R2_Root(String serviceurl, String serviceparam) throws IOException
	{
		  try 
		  {
			  String serviceid =  R2_GetID();
			  JSONObject newservice = new JSONObject();
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  newservice.put("service_type", "Root");
			  
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("r2_msg", newservice);
			  newrequest.put("root_service", serviceid);
			  newrequest.put("type", "Root");
			  String resp = SendEvent(newrequest.toString());	  
			  return resp;

		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException("Clean");
		  }
	}
	
	// //////////////////////////////////////////////////////
	//
	//
	// FLOW SERVICES
	//
	//
	// //////////////////////////////////////////////////////
	private String R2_FlowService(String serviceid, String serviceurl, String serviceparam, String servicetype) throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		  try 
		  {
			  String rootid = r2json.getString("root_service");
			  String parentid = r2json.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", parentid);
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  newservice.put("service_type", servicetype);
		
			  //newrequest.put("type", "Batch");
			  r2regarray.put(newservice);
		
			  return serviceid;
		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(r2json.toString());
		  }
	}


	// //////////////////////////////////////////////////////
	//
	// R2_Independant
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String R2_Independant(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2_FlowService(serviceid, serviceurl, serviceparam, "I");
	}	

	// without passed in serviceid
	public String R2_Independant(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2_GetID();
		return R2_FlowService(serviceid, serviceurl, serviceparam, "I");
	}	

	
	// //////////////////////////////////////////////////////
	//
	// R2_Contained
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String R2_Contained(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2_FlowService(serviceid, serviceurl, serviceparam, "C");
	}	

	// without passed in serviceid
	public String R2_Contained(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2_GetID();
		return R2_FlowService(serviceid, serviceurl, serviceparam, "C");
	}	

	// //////////////////////////////////////////////////////
	//
	// R2_Subsequent
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String R2_Subsequent(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2_FlowService(serviceid, serviceurl, serviceparam, "S");
	}	

	// without passed in serviceid
	public String R2_Subsequent(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2_GetID();
		return R2_FlowService(serviceid, serviceurl, serviceparam, "S");
	}	

	
	// //////////////////////////////////////////////////////
	//
	// R2_Final
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String R2_Final(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2_FlowService(serviceid, serviceurl, serviceparam, "F");
	}	

	// without passed in serviceid
	public String R2_Final(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2_GetID();
		return R2_FlowService(serviceid, serviceurl, serviceparam, "F");
	}	
	
	
    // //////////////////////////////////////////////////////
	//
	// R2_Setpredecessor
	//
    // //////////////////////////////////////////////////////
	public void R2_Setpredecessor(String preid, String postid) throws IOException
	{
	      //System.out.println(postid);
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		  try 
		  {
			  String rootid = r2json.getString("root_service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("service_type", "Pre");
			  newservice.put("root_service", rootid);
			  newservice.put("pre_service", preid);
			  newservice.put("blocked_service", postid);
			  r2regarray.put(newservice);
		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(r2json.toString());
		  }
	}	
	

    // //////////////////////////////////////////////////////
	//
	// R2_Release
	//
    // //////////////////////////////////////////////////////
	public String R2_Release() throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		  try 
		  {
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("r2_msg", r2regarray);
			  newrequest.put("type", "Batch");
			  newrequest.put("root_service", r2json.getString("root_service"));
			  newrequest.put("service", r2json.getString("service"));
			  String resp = SendEvent(newrequest.toString());	  
			  r2regarray = new JSONArray();
			  return resp.toString();

		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(r2json.toString());
		  }
	}



	// //////////////////////////////////////////////////////
	//
	// R2_Complete
	//
    // //////////////////////////////////////////////////////
	public String R2_Complete() throws IOException
	{
		if (r2json == null)
		{
			  throw new IOException("R2_Lib not initialized correctly, use the constructor with the R2_Jason parameter");			
		}
		  try 
		  {
			  String rootid = r2json.getString("root_service");
			  String partentid = r2json.getString("parent_service");
			  String eventid = r2json.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("service_type", "Update");
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", partentid);
			  newservice.put("service", eventid);
			  
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("r2_msg", newservice);
			  newrequest.put("type", "Complete");
			  newrequest.put("root_service", r2json.getString("root_service"));
		      //System.out.println("r2Update Sending: ");
			  String resp = SendEvent(newrequest.toString());	  
		     // System.out.println("r2Update Ending: ");
			  return resp.toString();

		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(r2json.toString());
		  }
	}



	// //////////////////////////////////////////////////////
	//
	// R2_Clean temp helper class to truncate db
	//
    // //////////////////////////////////////////////////////
	public String R2_Clean() throws IOException
	{
		  try 
		  {
			  JSONObject newservice = new JSONObject();
			  newservice.put("service_type", "Clean");
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("r2_msg", newservice);
			  newrequest.put("type", "Clean");
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();

		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException("Clean");
		  }
	}

	// //////////////////////////////////////////////////////
	//
	// SendEvent with timeout and retry
	//
    // //////////////////////////////////////////////////////
	private String SendEvent(String strparam) throws IOException
	{
		//HttpResponse  response;

		  StringBuffer resp = new StringBuffer();
		  
		  URL url = new URL(R2_URL);
		  int retries = R2_RETRIES;
		  String errorstring = "";
		  
		  while (retries > 0)
		  {
			  try 
			  {			  
				  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				  connection.setConnectTimeout(R2_TIMEOUT);		// TIMEOUT - server took to long to even accept the request
				  connection.setReadTimeout(R2_TIMEOUT);		// TIMEOUT - server accepted the request but taking too long, will through timeout exception
				  
				  // For a POST request
				  connection.setRequestMethod("POST");
				  connection.setRequestProperty("Content-Type", "application/json; utf-8");
				  connection.setDoOutput(true);

				  //System.out.println("R2Lib Sendevent Retries =  " + retries);
				  DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				  wr.writeBytes(strparam);
				  wr.flush();
				  wr.close();
				  int responseCode = connection.getResponseCode();
				  if (responseCode == 200)
				  {			  
					  //System.out.println("R2Lib Ret Code: " + responseCode);
					  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					  String output;
					 
					  while ((output = in.readLine()) != null) 
					  {
						  resp.append(output);
					  }
					  in.close();
					  connection.disconnect();
					  return resp.toString();
				  }
				  else
				  {
					  connection.disconnect();
					  errorstring = ("R2Lib Ret Code: " + responseCode);
					  //System.out.println("R2Lib Ret Code: " + responseCode);
					  //throw new IOException("IO Error sending to R2 ret code: " + responseCode);
					  retries--;
					  try 
					  {
						  TimeUnit.MILLISECONDS.sleep(R2_TIMEOUT_WAIT);	// add a little wait, to see if root will end
					  }
					  catch (JSONException | InterruptedException ie) 
					  {
						  /*report an error*/ 
						  // crash and burn
						  throw new IOException("R2Lib InterruptedException " + ie.toString());
					  }
					  
				  }
			  }
			  catch (java.net.SocketTimeoutException e) {
				  errorstring = ("R2Lib Timeout: " + e.toString());
				  //System.out.println("R2Lib Timeout: " + e.toString());
				  //throw new IOException("R2Lib Timeout: " + e.toString());	// catch TIMEOUT here
				  retries--;
				  try 
				  {
					  TimeUnit.MILLISECONDS.sleep(R2_TIMEOUT_WAIT);	// add a little wait, to see if root will end
				  }
				  catch (JSONException | InterruptedException ie) 
				  {
					  throw new IOException("R2Lib InterruptedException " + ie.toString());
				  }
			  }
			  catch (Exception e) 
			  { 
				  errorstring = ("R2Lib Exception: " + e.toString());
				  System.out.println("R2Lib Exception: " + e.toString());
				  retries--;
				  try 
				  {
					  TimeUnit.MILLISECONDS.sleep(R2_TIMEOUT_WAIT);	// add a little wait, to see if root will end
				  }
				  catch (JSONException | InterruptedException ie) 
				  {
					  throw new IOException("R2Lib InterruptedException " + ie.toString());
				  }
			  }
		  }
		  
		  
		  throw new IOException(errorstring);

	}

}
