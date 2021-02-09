package r2fileapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import com.datastax.driver.core.utils.UUIDs;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

//import java.util.UUID;

public class R2Lib {
	
	private JSONArray regarray = new JSONArray();

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
			  String serviceid =  RtoosGetID();
			  JSONObject newservice = new JSONObject();
			  newservice.put("type", "Root");
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("rtoos_msg", newservice);
			  newrequest.put("type", "Indvidual");
			  String resp = SendEvent(newrequest.toString());	  
			  return serviceid;

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
			  if (servicetype.equals("Register"))
			  {
				  //newrequest.put("type", "Batch");
				  regarray.put(newservice);
			  }
			  else
			  {
				  JSONObject newrequest = new JSONObject();
				  newrequest.put("rtoos_msg", newservice);
				  newrequest.put("type", "Indvidual");
				  String resp = SendEvent(newrequest.toString());	
			  }
			  return serviceid;
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
			  if (servicetype.equals("Register"))
			  {
				  //newrequest.put("type", "Batch");
				  regarray.put(newservice);
			  }
			  else
			  {
				  JSONObject newrequest = new JSONObject();
				  newrequest.put("rtoos_msg", newservice);
				  newrequest.put("type", "Indvidual");
				  String resp = SendEvent(newrequest.toString());	
			  }
			  return serviceid;
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
			  if (servicetype.equals("Register"))
			  {
				  //newrequest.put("type", "Batch");
				  regarray.put(newservice);
			  }
			  else
			  {
				  JSONObject newrequest = new JSONObject();
				  newrequest.put("rtoos_msg", newservice);
				  newrequest.put("type", "Indvidual");
				  String resp = SendEvent(newrequest.toString());	
			  }
			  return serviceid;
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
	public String RtoosPredecessor(String preid, String postid, String servicetype, JSONObject jsonrtoos) throws IOException
	{
	      //System.out.println(postid);
		  try 
		  {
			  String rootid = jsonrtoos.getString("root_service");
			
			  JSONObject newservice = new JSONObject();
			  newservice.put("service_type", servicetype);
			  newservice.put("type", "Pre");
			  newservice.put("root_service", rootid);
			  newservice.put("pre_service", preid);
			  newservice.put("blocked_service", postid);
			  if (servicetype.equals("Register"))
			  {
				  //newrequest.put("type", "Batch");
				  regarray.put(newservice);
			  }
			  else
			  {
				  JSONObject newrequest = new JSONObject();
				  newrequest.put("rtoos_msg", newservice);
				  newrequest.put("type", "Indvidual");
				  String resp = SendEvent(newrequest.toString());	
			  }
			  return preid;
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
	// RtoosRelease
	//
    // //////////////////////////////////////////////////////
	public String RtoosRelease(JSONObject jsonrtoos) throws IOException
	{
		  try 
		  {
			  JSONObject newrequest = new JSONObject();
			  newrequest.put("root_service", jsonrtoos.getString("root_service"));
			  newrequest.put("service", jsonrtoos.getString("service"));

			  newrequest.put("rtoos_msg", regarray);
			  newrequest.put("type", "Batch");
			  String resp = SendEvent(newrequest.toString());	  
			  regarray = new JSONArray();
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
			  newrequest.put("type", "Indvidual");
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
			  newrequest.put("type", "Indvidual");
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
		HttpResponse  response;
/*		
		try {

			DefaultHttpClient httpClient = new DefaultHttpClient();
		    StringEntity entity = new StringEntity(strparam);
		    HttpPost post  = new HttpPost("http://localhost:8080/R2FileApp/R2.html");
		    post.setEntity(entity);
		    post.setHeader("Accept", "application/json; utf-8");
		    post.setHeader("Content-type", "application/json; utf-8");
//		    post.setHeader("Connection", "close");
//		    post.setHeader("Connection", "keep-alive");

		    response = httpClient.execute(post);
		    httpClient.close();
		    //assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
		    //httpClient.close();
		}
		catch (Exception e)  { 
			  // crash and burn
			  throw new IOException("Error sending to Rtoos");
		}
		
	    return response.toString();
*/
		  StringBuffer resp = new StringBuffer();
		  try 
		  {
			  URL url = new URL("http://localhost:8080/R2FileApp/R2.html");
			  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			  // For a PUT request
			  connection.setRequestMethod("POST");
			  connection.setRequestProperty("Content-Type", "application/json; utf-8");
			  //connection.setRequestProperty("Connection", "keep-alive");
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
			  connection.disconnect();
		  }
		  catch (Exception e) 
		  { 
			  // crash and burn
			  throw new IOException("Error sending to Rtoos");
		  }
		  return resp.toString();

	}

}
