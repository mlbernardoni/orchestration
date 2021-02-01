package testapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class RtoosLib {
	
	public String RtoosIndependant(String eventurl, String eventparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String eventid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", servicetype);
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", eventid);
			  newservice.put("service_url", eventurl);
			  newservice.put("service_param", eventparam);
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
	public String RtoosContained(String eventurl, String eventparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String partentid = jsonrtoos.getString("parent_service");
			  String eventid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", servicetype);
			  newservice.put("root_service", rootid);
			  newservice.put("blocked_parent", partentid);
			  newservice.put("parent_service", eventid);
			  newservice.put("service_url", eventurl);
			  newservice.put("service_param", eventparam);
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
	
	public String RtoosSubsequent(String eventurl, String eventparam, String servicetype, String eventid, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", servicetype);
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", eventid);
			  newservice.put("service_url", eventurl);
			  newservice.put("service_param", eventparam);
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
	
	public String RtoosPredecessor(String preid, String eventid, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("root_service", rootid);
			  newservice.put("pre_service", preid);
			  newservice.put("blocked_service", eventid);
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
	
/*	
	public String RtoosNew(String eventurl, String eventparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String partentid = jsonrtoos.getString("parent_service");
			  String eventid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "New");
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", eventid);
			  if (servicetype == "C")
				  newservice.put("blocked_parent", partentid);
			  newservice.put("service_url", eventurl);
			  newservice.put("service_param", eventparam);
			  newservice.put("service_type", servicetype);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();
		  } 
		  catch (JSONException  e) 
		  {
			  // report an error
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}
	public String RtoosReg(String eventurl, String eventparam, String servicetype, JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			  String partentid = jsonrtoos.getString("parent_service");
			  String eventid = jsonrtoos.getString("service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Register");
			  newservice.put("root_service", rootid);
			  newservice.put("parent_service", eventid);
			  if (servicetype == "C")
				  newservice.put("blocked_parent", partentid);
			  newservice.put("service_url", eventurl);
			  newservice.put("service_param", eventparam);
			  newservice.put("service_type", servicetype);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  String resp = SendEvent(newrequest.toString());	  
			  return resp.toString();
		  } 
		  catch (JSONException  e) 
		  {
			  // report an error
			  // crash and burn
			  throw new IOException(jsonrtoos.toString());
		  }
	}
*/
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

	public String RtoosRoot(String eventurl, String eventparam) throws IOException
	{
		  try 
		  {
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Root");
			  newservice.put("service_url", eventurl);
			  newservice.put("service_param", eventparam);
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
			  URL url = new URL("http://localhost:8080/Rtoos/rtoos.html");
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
