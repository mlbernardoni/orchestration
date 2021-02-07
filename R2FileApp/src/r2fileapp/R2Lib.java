package r2fileapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.utils.UUIDs;

//import java.util.UUID;

public class R2Lib {

    // //////////////////////////////////////////////////////
	//
	// returns an id
	//
    // //////////////////////////////////////////////////////
	public String RtoosGetID() 
	{
		return UUIDs.random().toString();
	}


    // //////////////////////////////////////////////////////
	//
	// RtoosRoot
	//
    // //////////////////////////////////////////////////////
	public String RtoosRoot(String serviceurl, String serviceparam) throws IOException
	{
		  try 
		  {
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Root");
			  newservice.put("service", RtoosGetID());
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();

		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException("Clean");
		  }
	}

	// //////////////////////////////////////////////////////
	//
	// RtoosIndependant
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String RtoosIndependant(String serviceid, String serviceurl, String serviceparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String parentid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", servicetype);
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", parentid);
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  newservice.put("service_type", "I");
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	

	// without passed in serviceid
	public String RtoosIndependant(String serviceurl, String serviceparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  String serviceid = RtoosGetID();
		  try 
		  {
			  return RtoosIndependant(serviceid, serviceurl, serviceparam, servicetype, jsonrtoos);
		  }
		  catch (IOException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	

	
    // //////////////////////////////////////////////////////
	//
	// RtoosContained
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String RtoosContained(String serviceid, String serviceurl, String serviceparam, String servicetype, JSONObject jsonrtoos) throws IOException
		{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String parentid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", servicetype);
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", parentid);
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  newservice.put("service_type", "C");
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	
	
	// without passed in serviceid
	public String RtoosContained(String serviceurl, String serviceparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  String serviceid = RtoosGetID();
		  try 
		  {
			  return RtoosContained(serviceid, serviceurl, serviceparam, servicetype, jsonrtoos);
		  }
		  catch (IOException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	
	
	
    // //////////////////////////////////////////////////////
	//
	// RtoosSubsequent
	// with passed in serviceid
	//
    // //////////////////////////////////////////////////////
	public String RtoosSubsequent(String serviceid, String serviceurl, String serviceparam, String servicetype, String preid, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", servicetype);
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", preid);
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  newservice.put("service_type", "S");
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	
	
	// without passed in serviceid
	public String RtoosSubsequent(String serviceurl, String serviceparam, String servicetype, String preid, JSONObject jsonrtoos) throws IOException
	{
		  String serviceid = RtoosGetID();
		  try 
		  {
			  return RtoosSubsequent(serviceid, serviceurl, serviceparam, servicetype, preid, jsonrtoos);
		  }
		  catch (IOException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	
	
    // //////////////////////////////////////////////////////
	//
	// RtoosPredecessor
	//
    // //////////////////////////////////////////////////////
	public String RtoosPredecessor(String preid, String postid, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Pre");
			  newservice.put("root_service", rootid);
			  newservice.put("pre_service", preid);
			  newservice.put("blocked_service", postid);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}	
	

    // //////////////////////////////////////////////////////
	//
	// RtoosUpdate
	//
    // //////////////////////////////////////////////////////
	public String RtoosUpdate(String status, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String partentid = jsonrtoos.getString("parent_service");
			  String eventid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Update");
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", partentid);
			  newservice.put("service", eventid);
			  newservice.put("status", status);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();

		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}


	public String RtoosClean() throws IOException
	{
		  try 
		  {
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Clean");
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();

		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException("Clean");
		  }
	}

	private String SendEvent(String strparam) throws IOException
	{
		
		  StringBuffer resp = new StringBuffer();
		  try 
		  {
			  URL url = new URL("http://localhost:8080/R2FileApp/R2.html");
			  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			  // For a PUT request
			  connection.setRequestMethod("POST");
			  connection.setRequestProperty("Content-Type", "application/json; utf-8");
			  connection.setDoOutput(true);
			  DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			  wr.writeBytes(strparam);
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
			  throw new IOException("Error sending to Rtoos");
		  }
		  return resp.toString();
	}

}
