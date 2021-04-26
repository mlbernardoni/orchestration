package R2sLib;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * R2sLib is the client side library for the R2s platform
 * 
 * @author mlbernardoni
 *
 */
public class R2sLib {

	private JSONArray r2regarray = new JSONArray();
	private JSONObject r2json = null;
	private int R2s_TIMEOUT = 600000;
	private int R2s_TIMEOUT_WAIT = 3000;
	private int R2s_TRIES = 3;
	private String R2s_URL = "http://localhost:8080/R2s/R2s.html";
	
	/**
	 * R2sLib Constructor used to start an R2s service tree
	 *  With R2sLib constructed with no parameters, the only functionality available is R2sRoot
	 */
	public R2sLib()
	{
		R2s_Init();
	}

	/**
	 * R2sLib Constructor used within an R2s service tree
	 * @param jb - the string retrieved from the HTTP Post request from R2s; allows full functionality of the library 
	 * @throws IOException an incompatible request string was provided
	 */
	public R2sLib(String jb) throws IOException
	{
		try {
			JSONObject r2_json =  new JSONObject(jb);
			r2json = (JSONObject)r2_json.get("r2_msg");
			R2s_Init();
		}
	    catch (JSONException  e) 
	    {
	    	throw new IOException(jb + e.toString());
	    }
	}
	
	private void R2s_Init()
	{
		try {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();           
				InputStream stream = classLoader.getResourceAsStream("../R2sConfiguration.json");
				if (stream == null) {
				    System.out.println("R2sConfiguration.json missing from WEB-INF folder");
					return;
				}
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				for (int length; (length = stream.read(buffer)) != -1; ) {
				     result.write(buffer, 0, length);
				}
				// StandardCharsets.UTF_8.name() > JDK 7
				JSONObject r2sconifg =  new JSONObject(result.toString("UTF-8"));
				R2s_TIMEOUT = r2sconifg.getInt("R2s_TIMEOUT");
				R2s_TIMEOUT_WAIT = r2sconifg.getInt("R2s_TIMEOUT_WAIT");
				R2s_TRIES = r2sconifg.getInt("R2s_TRIES");
				R2s_URL = r2sconifg.getString("R2s_URL");
			} 
		catch (IOException e) {
		      System.out.println(e.toString());
			}
	}
	
	/**
	 * <p> R2s_SetTimeout, R2s_SetTimeoutWait, R2s_SetTries <br>
	 * the trio of functions used to change the default settings the client uses to call the R2s Server <br><br>
	 * NOTE: to change the default settings used by the R2s Server when calling a service, see the functions for registering a service
	 * </p>
	 * 
	 * @param newtimeout number of milliseconds before the call to R2s times out
	 */
	public void R2s_SetTimeout(int newtimeout) 
	{
		R2s_TIMEOUT = newtimeout;
	}
	/**
	 * 
	 * <p> R2s_SetTimeout, R2s_SetTimeoutWait, R2s_SetTries <br>
	 * the trio of functions used to change the default settings the client uses to call the R2s Server <br><br>
	 * NOTE: to change the default settings used by the R2s Server when calling a service, see the functions for registering a service
	 * </p>
	 * 
	 * @param newwait number of milliseconds to wait between attempted calls to the R2s Server
	 */
	public void R2s_SetTimeoutWait(int newwait) 
	{
		R2s_TIMEOUT_WAIT = newwait;
	}
	/**
	 * 
	 * <p> R2s_SetTimeout, R2s_SetTimeoutWait, R2s_SetTries <br>
	 * the trio of functions used to change the default settings the client uses to call the R2s Server <br><br>
	 * NOTE: to change the default settings used by the R2s Server when calling a service, see the functions for registering a service
	 * </p>
	 * 
	 * @param newtries number to tries to connect to the server (note 0 will not call the server)
	 */
	public void R2s_SetTries(int newtries) 
	{
		R2s_TRIES = newtries;
	}

	
	/**
	 * 
	 * <p> R2s_GetID <br>
	 * an function that is available to a client service, that gets an R2s compatible ID. <br> This is available if a service wants to use an ID for another use 
	 * but also register a service using that ID. <br>
	 * For example, if a service wishes to use an ID as a file key in a database,
	 * the service would call R2s_GetID to get an ID, use that ID as the file ID in its own database, 
	 * then register a service (such as "ParseFile") using that ID. This would in effect pass the file ID to the ParseFile service.
	 * </p>
	 * 
	 * @return an R2s compatible ID that can be passed into a register function
	 */
	public String R2s_GetID() 
	{
		return UUID.randomUUID().toString();
	}


	/**
	 * 
	 * <p> R2s_GetParam,  R2s_GetRootID, R2s_GetParentID, R2s_GetServiceID<br>
	 * The set of functions available to retrieve the data that was passed by the R2s Service 
	 * </p>
	 * 
	 * @return the parameter passed by the R2s Service
	 * @throws IOException invalid parameters were received in the request object
	 */
	public String R2s_GetParam()  throws IOException
	{
		R2s_IsValidJson(); 
		return r2json.getString("service_param");
	}
	/**
	 * 
	 * <p> R2s_GetParam,  R2s_GetRootID, R2s_GetParentID, R2s_GetServiceID<br>
	 * The set of functions available to retrieve the data that was passed by the R2s Service 
	 * </p>
	 * 
	 * @return the Root Service ID
	 * @throws IOException invalid parameters were received in the request object
	 */
	public String R2s_GetRootID()  throws IOException
	{
		R2s_IsValidJson(); 
		return r2json.getString("root_service");
	}
	/**
	 * 
	 * <p> R2s_GetParam,  R2s_GetRootID, R2s_GetParentID, R2s_GetServiceID<br>
	 * The set of functions available to retrieve the data that was passed by the R2s Service 
	 * </p>
	 * 
	 * @return the Parent Service ID
	 * @throws IOException invalid parameters were received in the request object
	 */
	public String R2s_GetParentID()  throws IOException
	{
		R2s_IsValidJson(); 
		return r2json.getString("parent_service");
	}
	/**
	 * 
	 * <p> R2s_GetParam,  R2s_GetRootID, R2s_GetParentID, R2s_GetServiceID<br>
	 * The set of functions available to retrieve the data that was passed by the R2s Service 
	 * </p>
	 * 
	 * @return the Service ID
	 * @throws IOException invalid parameters were received in the request object
	 */
	public String R2s_GetServiceID() throws IOException
	{
		R2s_IsValidJson(); 
		return r2json.getString("service");
	}
	
	private void R2s_IsValidJson() throws IOException{
		if (r2json == null)
		{
			  throw new IOException("R2s_Lib not initialized correctly, use the constructor with the HTTP Post request string parameter");			
		}
	}

	/**
	 * 
	 * <p> R2s_Root<br>
	 * Creates a new R2s Service Tree. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Root(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_Root(serviceid, serviceurl, serviceparam);
	}	

	/**
	 * 
	 * <p> R2s_Root with supplied ID retrieved from GetID<br>
	 * Creates a new R2s Service Tree. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Root(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		  try 
		  {
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
	/**
	 * 
	 * <p> R2s_Root with retry parameters<br>
	 * Creates a new R2s Service Tree. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Root(String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_Root(serviceid, serviceurl, serviceparam, tries, timeout, timeoutwait);
	}	
	/**
	 * 
	 * <p> R2s_Root with supplied ID retrieved from GetID and with retry parameters<br>
	 * Creates a new R2s Service Tree. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Root(String serviceid, String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		  try 
		  {
			  JSONObject newservice = new JSONObject();
			  newservice.put("service", serviceid);
			  newservice.put("service_url", serviceurl);
			  newservice.put("service_param", serviceparam);
			  newservice.put("service_type", "Root");
			  newservice.put("tries", tries);
			  newservice.put("timeout", timeout);
			  newservice.put("timeoutwait", timeoutwait);
			  
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
	private String R2s_FlowService(String serviceid, String serviceurl, String serviceparam, String servicetype, int tries, int timeout, int timeoutwait) throws IOException
	{
		R2s_IsValidJson(); 
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
			  newservice.put("tries", tries);
			  newservice.put("timeout", timeout);
			  newservice.put("timeoutwait", timeoutwait);
		
			  //newrequest.put("type", "Batch");
			  r2regarray.put(newservice);
		
			  return serviceid;
		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(r2json.toString());
		  }
	}
	private String R2s_FlowService(String serviceid, String serviceurl, String serviceparam, String servicetype) throws IOException
	{
		R2s_IsValidJson(); 
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
	// R2s_Independent
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Independent<br>
	 * Creates an independent service as a child service. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Independent(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "I");
	}	
	/**
	 * 
	 * <p> R2s_Independent<br>
	 * Creates an independent service as a child service with supplied ID retrieved from GetID. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Independent(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "I");
	}	
	/**
	 * 
	 * <p> R2s_Independent<br>
	 * Creates an independent service as a child service with retry parameters. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Independent(String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "I", tries, timeout, timeoutwait);
	}	
	/**
	 * 
	 * <p> R2s_Independent<br>
	 * Creates an independent service as a child service with supplied ID retrieved from GetID and with retry parameters <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Independent(String serviceid, String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "I", tries, timeout, timeoutwait);
	}	

	// //////////////////////////////////////////////////////
	//
	// R2s_Contained
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Contained<br>
	 * Creates an contained service as a child service. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Contained(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "C");
	}	
	/**
	 * 
	 * <p> R2s_Contained<br>
	 * Creates an contained service as a child service with supplied ID retrieved from GetID. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Contained(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "C");
	}	
	/**
	 * 
	 * <p> R2s_Contained<br>
	 * Creates an contained service as a child service with retry parameters. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Contained(String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "C", tries, timeout, timeoutwait);
	}	
	/**
	 * 
	 * <p> R2s_Contained<br>
	 * Creates an contained service as a child service with supplied ID retrieved from GetID and with retry parameters <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Contained(String serviceid, String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "C", tries, timeout, timeoutwait);
	}	


	// //////////////////////////////////////////////////////
	//
	// R2s_Subsequent
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Subsequent<br>
	 * Creates an subsequent service as a child service. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Subsequent(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "S");
	}	
	/**
	 * 
	 * <p> R2s_Subsequent<br>
	 * Creates an subsequent service as a child service with supplied ID retrieved from GetID. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Subsequent(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "S");
	}	
	/**
	 * 
	 * <p> R2s_Subsequent<br>
	 * Creates an subsequent service as a child service with retry parameters. <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Subsequent(String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "S", tries, timeout, timeoutwait);
	}	
	/**
	 * 
	 * <p> R2s_Subsequent<br>
	 * Creates an subsequent service as a child service with supplied ID retrieved from GetID and with retry parameters <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Subsequent(String serviceid, String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "S", tries, timeout, timeoutwait);
	}	

	
	// //////////////////////////////////////////////////////
	//
	// R2s_Final
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Final<br>
	 * Creates an on final service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on final service will be called when the service and all child services are final <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Final(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "F");
	}	
	/**
	 * 
	 * <p> R2s_Final<br>
	 * Creates an on final service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on final service will be called when the service and all child services are final <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Final(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "F");
	}	
	/**
	 * 
	 * <p> R2s_Final<br>
	 * Creates an on final service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on final service will be called when the service and all child services are final <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Final(String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "F", tries, timeout, timeoutwait);
	}	
	/**
	 * 
	 * <p> R2s_Final<br>
	 * Creates an on final service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on final service will be called when the service and all child services are final <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Final(String serviceid, String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "F", tries, timeout, timeoutwait);
	}	
	
	
	// //////////////////////////////////////////////////////
	//
	// R2s_Error
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Error<br>
	 * Creates an on error service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on error service will only be called when the service or any of its child services error <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Error(String serviceurl, String serviceparam) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "E");
	}	
	/**
	 * 
	 * <p> R2s_Error<br>
	 * Creates an on error service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on error service will only be called when the service or any of its child services error <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Error(String serviceid, String serviceurl, String serviceparam) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "E");
	}	
	/**
	 * 
	 * <p> R2s_Error<br>
	 * Creates an on error service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on error service will only be called when the service or any of its child services error <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Error(String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		String serviceid = R2s_GetID();
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "E", tries, timeout, timeoutwait);
	}	
	/**
	 * 
	 * <p> R2s_Error<br>
	 * Creates an on error service as a child service with supplied ID retrieved from GetID and with retry parameters.
	 * An on error service will only be called when the service or any of its child services error <br>
	 * All R2sLib register services must include: <br>
	 * The URL of the service to add to the tree <br>
	 * The Parameter passed to that service <br>
	 * Optionally an ID retrieved from GetID (see GetID for details) <br>
	 * By default R2s calls the service at most once, if the service is idempotent
	 * optional parameters are available to instruct R2s to include retry logic while
	 * while calling the service
	 * </p>
	 * 
	 * @param serviceid ID the application retrieved by using the GetID method
	 * @param serviceurl The URL of the Root Service
	 * @param serviceparam The parameter passed into the Service
	 * @param tries The number of attempts the R2s server will use while calling the service
	 * @param timeout The timeout in milliseconds the R2s Server will wait on a service call
	 * @param timeoutwait The time in milliseconds that the R2s Server will wait between attemps
	 * @return The ID of the registered Root Service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Error(String serviceid, String serviceurl, String serviceparam, int tries, int timeout, int timeoutwait) throws IOException
	{
		return R2s_FlowService(serviceid, serviceurl, serviceparam, "E", tries, timeout, timeoutwait);
	}	
	
	
    // //////////////////////////////////////////////////////
	//
	// R2s_Setpredecessor
	//
    // //////////////////////////////////////////////////////
	/**
	 * <p> R2s_Setpredecessor<br>
	 * Chains two services together as a predecessor and successor service. A common pattern is to create 2 independant services
	 * and chain them using R2s_Setpredecessor. <br>
	 * This allows a service to create service chains (for example, validate transaction, authorize transaction, complete transaction).
	 * </p>
	 * 
	 * @param preid The ID of the predecessor service
	 * @param postid The ID of the successor service
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public void R2s_Setpredecessor(String preid, String postid) throws IOException
	{
	      //System.out.println(postid);
		R2s_IsValidJson(); 
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
	// R2s_Release
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Release<br>
	 * After registering child services, a service will call R2s_Release
	 * which will call the R2s Server to process the service sub-tree.
	 * </p>
	 * 
	 * @return a success message
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Release() throws IOException
	{
		R2s_IsValidJson(); 
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
	// R2s_Complete
	//
    // //////////////////////////////////////////////////////
	/**
	 * 
	 * <p> R2s_Complete<br>
	 * When finished processing, the service will call R2s_Complete
	 * which informs the R2s Server that processing is complete
	 * and any child processes can be initiated.
	 * </p>
	 * 
	 * @return a success message
	 * @throws IOException Error creating the JSON object to pass to the R2s Service
	 */
	public String R2s_Complete() throws IOException
	{
		R2s_IsValidJson(); 
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


/*
	// //////////////////////////////////////////////////////
	//
	// R2s_Clean temp helper class to truncate db
	//
    // //////////////////////////////////////////////////////
	public String R2s_Clean() throws IOException
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
*/
	// //////////////////////////////////////////////////////
	//
	// SendEvent with timeout and retry
	//
    // //////////////////////////////////////////////////////
	private String SendEvent(String strparam) throws IOException
	{
		//HttpResponse  response;

		  StringBuffer resp = new StringBuffer();
		  
		  URL url = new URL(R2s_URL);
		  int tries = R2s_TRIES;
		  String errorstring = "";
		  
		  while (tries > 0)
		  {
			  try 
			  {			  
				  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				  connection.setConnectTimeout(R2s_TIMEOUT);		// TIMEOUT - server took to long to even accept the request
				  connection.setReadTimeout(R2s_TIMEOUT);		// TIMEOUT - server accepted the request but taking too long, will through timeout exception
				  
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
					  tries--;
					  try 
					  {
						  TimeUnit.MILLISECONDS.sleep(R2s_TIMEOUT_WAIT);	// add a little wait, to see if root will end
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
				  tries--;
				  try 
				  {
					  TimeUnit.MILLISECONDS.sleep(R2s_TIMEOUT_WAIT);	// add a little wait, to see if root will end
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
				  tries--;
				  try 
				  {
					  TimeUnit.MILLISECONDS.sleep(R2s_TIMEOUT_WAIT);	// add a little wait, to see if root will end
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
