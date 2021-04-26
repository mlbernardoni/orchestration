package R2s;

import java.io.BufferedReader;
import java.util.concurrent.*;
import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;

import java.util.ArrayList;



/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/R2s")
public class R2s extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static Cluster r2scluster;
	public static Semaphore mysemaphore;
	
	private static int R2s_TRIES  = 1;
	private static int R2s_TIMEOUT  = 600000;
	private static int R2s_TIMEOUTWAIT  = 10000;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public R2s() {
        super();
    }

    // called by watchdog when Final or Error sends succeed
    // as these may or may not R2s endpoints, they may not call complete
    // and if they do, no big deal
    public static void MarkComplete(JSONObject myrow) throws IOException {
 	    String rootid = myrow.getString("root_service");
  	    String eventid = myrow.getString("service");
  	    R2s_DAL dal = new R2s_DAL();
  	    dal.Init();
	    dal.RetrieveServiceTree(rootid);
  	    JSONObject row = dal.GetServiceRow(eventid);    
		row.put("status", "F");
		dal.UpdateServiceRow(row);		  	      
	    dal.CleanUp(); 
    }

    // called by watchdog when an error is detected
    // marks the service as error and checks for an onerror function
   public static void OnError(JSONObject myrow, String errorstring) throws IOException {
    	String err = "R2s Error: " + errorstring;
		System.out.println(err);    	
  	    R2s_DAL dal = new R2s_DAL();
  	    dal.Init();
 	    String rootid = myrow.getString("root_service");
 	    String partentid = myrow.getString("parent_service");
 	    String eventid = myrow.getString("service");
 	    dal.RetrieveServiceTree(rootid);
  	    JSONObject row = dal.GetServiceRow(eventid);
	  
		row.put("status", "E");
		row.put("errorstring", errorstring);
		dal.UpdateServiceRow(row);		  	      

		String testparent = partentid;
		String testservice = eventid;
		while (testservice != "")
		{
			  ArrayList<JSONObject> childlist = dal.GetServiceChildren(testservice);
		      for (int i = 0; i < childlist.size(); i++)
		      {
		    	  JSONObject item = childlist.get(i);
			      String servicetype = item.getString("servicetype");	
			      String status = item.getString("status");
		    	  if (servicetype.equals("E") && status.equals("R"))
		    	  {
		    			if (dal.UpdateSendStatus(item, true))
		    			{
		    				  item.put("service_param", myrow.toString());
		    			      R2SendThread T1 = new R2SendThread( item, mysemaphore);
		    				  Thread t = new Thread (T1, "SendThread");					  
		    			      t.start();
		    			}
		    			else
		    				System.out.println(errorstring);	  
		    	  }
		      
		      }
		      if (testservice.equals(rootid))
		      {
		    	  testservice = "";
		      }
		      else
		      {
		    	  testservice = testparent;
		    	  row = dal.GetServiceRow(testservice);
		    	  testparent = row.getString("parent_service");
		      }
		
		}
  
		dal.CleanUp(); 
    }

    public void init(ServletConfig config) throws ServletException {
    	r2scluster = Cluster.builder().addContactPoint("127.0.0.1").build();
   	  	
   	  	mysemaphore = new Semaphore(150);	// must be > 1
    }
    
    public void destroy() {
    	
    	r2scluster.close();	// not sure this does anything
   }
    
	protected  void sendEvent(String rootid, String serviceid, boolean consensus, R2s_DAL dal) throws IOException {
		
  	  JSONObject row = dal.GetServiceRow(serviceid);

		if (dal.UpdateSendStatus(row, consensus))
		{
			
		  
		  try {
			  //mysemaphore.acquire();
		      R2sWatchDog T1 = new R2sWatchDog( row, mysemaphore);
				  
		      T1.start();
		  }
		  catch (Exception e) { 
				throw new IOException("Error starting watchdog");
		  }   	  
		}
		else
			System.out.println("OY2");	  
}

	
	// /////////////////////////////////////////////
	//
	//	DoRoot
	//
	// /////////////////////////////////////////////
	protected String DoRoot(JSONObject jsonrtoos, R2s_DAL dal) throws IOException {

		JSONObject newobj = new JSONObject();
		String eventid = jsonrtoos.getString("service");

		// Prevents duplicate root object from being created
		if (dal.GetServiceRow(eventid) != null) {
			return eventid;
		}

		newobj.put("root_service", eventid);
		newobj.put("parent_service", eventid);
		newobj.put("service", eventid);
		String serviceurl = jsonrtoos.getString("service_url");
		newobj.put("service_url", serviceurl);
		String serviceparam = jsonrtoos.getString("service_param");
		newobj.put("service_param", serviceparam);
		newobj.put("status", "R");
		newobj.put("servicetype", "R");

		  int temptries = R2s_TRIES;
		  if (jsonrtoos.has("tries"))
			  temptries = jsonrtoos.getInt("tries");
		  newobj.put("tries", temptries);
		  int temptimeout = R2s_TIMEOUT;
		  if (jsonrtoos.has("timeout"))
			  temptimeout = jsonrtoos.getInt("timeout");
		  newobj.put("timeout", temptimeout);
		  int temptimeoutwait = R2s_TIMEOUTWAIT;
		  if (jsonrtoos.has("timeoutwait"))
			  temptimeoutwait = jsonrtoos.getInt("timeoutwait");
		  newobj.put("timeoutwait", temptimeoutwait);
		  
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());		  
		newobj.put("create_date", timestamp.toString());
		//System.out.println(timestamp.toString());	  

		dal.UpdateServiceRow(newobj);

		sendEvent(eventid, eventid, false, dal);
		return eventid;
	}

	// /////////////////////////////////////////////
	//
	//	DoPre
	//
	// /////////////////////////////////////////////
	protected String DoPre ( JSONObject jsonrtoos, R2s_DAL dal) throws IOException {
		  String rootid = jsonrtoos.getString("root_service");
		  String preid = jsonrtoos.getString("pre_service");
		  String eventid = jsonrtoos.getString("blocked_service");

		  JSONObject newobj = new JSONObject();		
		  newobj.put("root_service", rootid);
		  newobj.put("pre_service", preid);
		  newobj.put("blocked_service", eventid);
		  newobj.put("status", "W");		  
		  dal.UpdateBlockedRow(newobj);

		  JSONObject row = dal.GetServiceRow(eventid);		  
		  row.put("servicetype", "S");
		  row.put("parent_service", preid);
		  dal.UpdateServiceRow(row);

		  return eventid.toString();
	}

	
	// /////////////////////////////////////////////
	//
	//	DoNew
	//
	// /////////////////////////////////////////////
	protected String DoNew ( JSONObject jsonrtoos, R2s_DAL dal) throws IOException {
		
		  JSONObject newobj = new JSONObject();		
		  String rootid = jsonrtoos.getString("root_service");
		  newobj.put("root_service", rootid);
		  String partentid = jsonrtoos.getString("parent_service");
		  newobj.put("parent_service", partentid);
	      String eventid = jsonrtoos.getString("service");
		  newobj.put("service", eventid);
		  String serviceurl = jsonrtoos.getString("service_url");
		  newobj.put("service_url", serviceurl);
		  String serviceparam = jsonrtoos.getString("service_param");
		  newobj.put("service_param", serviceparam);
		  newobj.put("status", "R");
		  String servicetype = jsonrtoos.getString("service_type");
		  newobj.put("servicetype", servicetype);
		  int temptries = R2s_TRIES;
		  if (jsonrtoos.has("tries"))
			  temptries = jsonrtoos.getInt("tries");
		  newobj.put("tries", temptries);
		  int temptimeout = R2s_TIMEOUT;
		  if (jsonrtoos.has("timeout"))
			  temptimeout = jsonrtoos.getInt("timeout");
		  newobj.put("timeout", temptimeout);
		  int temptimeoutwait = R2s_TIMEOUTWAIT;
		  if (jsonrtoos.has("timeoutwait"))
			  temptimeoutwait = jsonrtoos.getInt("timeoutwait");
		  newobj.put("timeoutwait", temptimeoutwait);
		  Timestamp timestamp = new Timestamp(System.currentTimeMillis());		  
		  newobj.put("create_date", timestamp.toString());
		  
		  dal.UpdateServiceRow(newobj);
		  
	      if (servicetype.equals("S"))	// successor, add eventid to blocked table as waiting (waiting for parent)
	      {
			  JSONObject blockobj = new JSONObject();		
			  blockobj.put("root_service", rootid);
			  blockobj.put("pre_service", partentid);
			  blockobj.put("blocked_service", eventid);
			  blockobj.put("status", "W");		  
			  dal.UpdateBlockedRow(blockobj);
	      }
	      else if (servicetype.equals("C"))	// contained, add parent to blocked table (blocked by eventid)
	      {
			  JSONObject blockobj = new JSONObject();		
			  blockobj.put("root_service", rootid);
			  blockobj.put("pre_service", eventid);
			  blockobj.put("blocked_service", partentid);
			  blockobj.put("status", "B");		  
			  dal.UpdateBlockedRow(blockobj);
	      }				      
	      else if (servicetype.equals("F"))	// contained, add parent to blocked table (blocked by eventid)
	      {
			  JSONObject blockobj = new JSONObject();		
			  blockobj.put("root_service", rootid);
			  blockobj.put("pre_service", partentid);
			  blockobj.put("blocked_service", eventid);
			  blockobj.put("status", "F");		  
			  dal.UpdateBlockedRow(blockobj);
	      }				      
		  return eventid;
	}
	

	// /////////////////////////////////////////////
	//
	//	DoFinal
	//
	// /////////////////////////////////////////////
	protected void DoFinal ( JSONObject jsonrtoos, R2s_DAL dal) throws IOException {
 	  	  String rootid = jsonrtoos.getString("root_service");
		  String partentid = jsonrtoos.getString("parent_service");
		  String eventid = jsonrtoos.getString("service");
		  
		  String testparent = partentid;
		  String testservice = eventid;
		  while (testservice != "")
		  {
	    	  ArrayList<JSONObject> all2 = dal.GetServiceChildren(testservice);
			  // see if any not finished
		      int notfinished = 0;
		      if (all2 != null)
		      {
			      for (int i = 0; i < all2.size() && notfinished == 0; i++)
			      {
				      String status = all2.get(i).getString("status");	
				      String servicetype = all2.get(i).getString("servicetype");
				      String service = all2.get(i).getString("service");
				      if (!status.equals("F") && !service.equals(rootid) && !servicetype.equals("E") && !servicetype.equals("F"))	// got a status that is not finished
				    	  notfinished = 1;
			      }
		      }

		      // this one is not finished so end
		      if (notfinished == 1 )
		    	  break;

		      // mark row as finished
	    	  JSONObject row = dal.GetServiceRow(testservice);
			  row.put("status", "F");
			  dal.UpdateServiceRow(row);
			  // in complete and final, we can have concurent calls hitting the chain
			  // for example, multiple services completeing the same time
			  // so always to a read after a write in complete and final
			  dal.UpdateServices();
				  			      
		      // see if service is set to run on finished
			  ArrayList<JSONObject> blockedlist = dal.GetBlockedServices(testservice);
		      for (int i = 0; i < blockedlist.size(); i++)
		      {
		    	  JSONObject item = blockedlist.get(i);
			      String status = item.getString("status");	
		    	  if (status.equals("F"))
		    	  {
				      String blockedservice = item.getString("blocked_service");	
			    	  JSONObject blockedrow = dal.GetBlockedRow(testservice, blockedservice);
			    	  blockedrow.put("status", "C");
					  dal.UpdateBlockedRow(blockedrow);	
					  // in complete and final, we can have concurent calls hitting the chain
					  // for example, multiple services completeing the same time
					  // so always to a read after a write in complete and final
					  dal.UpdateBlocked(); 
					  
		    		  // run it
					  sendEvent( rootid,    blockedservice, true, dal);
		    	  }
		      }		    	  
		      if (testservice.equals(rootid))
		      {
		    	  testservice = "";
		      }
		      else
		      {
		    	  testservice = testparent;
		    	  row = dal.GetServiceRow(testservice);
		    	  testparent = row.getString("parent_service");
		      }
		  }
	}
	
	// /////////////////////////////////////////////
	//
	//	DoComplete
	//
	// /////////////////////////////////////////////
	protected void DoComplete ( JSONObject jsonrtoos, R2s_DAL dal) throws IOException {

		  String rootid = jsonrtoos.getString("root_service");
		  String eventid = jsonrtoos.getString("service");
		  
    	  JSONObject row = dal.GetServiceRow(eventid);
		  
		  row.put("status", "C");
		  dal.UpdateServiceRow(row);		  	      
		  // in complete and final, we can have concurent calls hitting the chain
		  // for example, multiple services completeing the same time
		  // so always to a read after a write in complete and final
		  dal.UpdateServices();
		  
		  //
		  // first things first, check to see if there are any contained
		  // that are keeping me from really completing
		  ArrayList<JSONObject> blockedlist = dal.GetPreServices(eventid);
	      int stillwaiting = 0;
	      for (int i = 0; i < blockedlist.size(); i++)
	      {
	    	  JSONObject item = blockedlist.get(i);
		      String mystatus = item.getString("status");	
		      if (mystatus.equals("B"))
		      {
		    	  stillwaiting = 1;					    	  
		      }
	      }
	  
	      if (stillwaiting == 1) 
	      {
			  //DoFinal( jsonrtoos, dal);	// no need to do a final here, we know that a process is running
	    	  return;
	      }
	      
	      //
	      // ok, we know that this service is really done (no contained)
	      // see if anyone waiting on this
		  blockedlist = dal.GetBlockedServices(eventid);
	      for (int i = 0; i < blockedlist.size(); i++)
	      {
	    	  JSONObject item = blockedlist.get(i);
		      String blocked_service = item.getString("blocked_service");
		      String blocked_status = item.getString("status");	
		      
		      //
		      // we have one waiting (a subsequent)
		      if(blocked_status.equals("W"))
		      {
		    	  // mark this blocked row as complete
		    	  // note marking the row in the blocked list, not the service
		    	  // as complete
		    	  JSONObject blockedrow = dal.GetBlockedRow(eventid, blocked_service);
		    	  blockedrow.put("status", "C");
				  dal.UpdateBlockedRow(blockedrow);		  
				  // in complete and final, we can have concurent calls hitting the chain
				  // for example, multiple services completeing the same time
				  // so always to a read after a write in complete and final
				  dal.UpdateBlocked();
				  
				  // see if any contained that this service has to wait for
				  // if not send the event
				  ArrayList<JSONObject> blockedlist2 = dal.GetPreServices(blocked_service);
			      int notblocked = 0;
			      for (int ii = 0; ii < blockedlist2.size(); ii++)
			      {
			    	  JSONObject item2 = blockedlist2.get(ii);
				      String stillblocked = item2.getString("status");	
				      if (!stillblocked.equals("C"))
				      {
				    	  notblocked = 1;	
				      }
			      }
		    	  if (notblocked == 0) // simple case, parent finished, can release
		    	  {
		    		  if ( blockedlist2.size() > 1)
		    			  sendEvent( rootid, blocked_service, true, dal);			
		    		  else
		    			  sendEvent( rootid, blocked_service, false, dal);			// no need for consensus if I am the only one
		    	  }				    	  				  
		      }
		      //
		      // there is one that is blocked by it (contained)
		      else if (blocked_status.equals("B")) // 
		      {
		    	  // mark this blocked row as complete
		    	  // note marking the row in the blocked list, not the service
		    	  // as complete
		    	  JSONObject blockedrow = dal.GetBlockedRow(eventid, blocked_service);
		    	  blockedrow.put("status", "C");
				  dal.UpdateBlockedRow(blockedrow);		
				  // in complete and final, we can have concurent calls hitting the chain
				  // for example, multiple services completeing the same time
				  // so always to a read after a write in complete and final
				  dal.UpdateBlocked();

				  // see if any contained that this service has to wait for
				  ArrayList<JSONObject> blockedlist2 = dal.GetPreServices(blocked_service);
			      int notblocked = 0;
			      for (int ii = 0; ii < blockedlist2.size(); ii++)
			      {
			    	  JSONObject item2 = blockedlist2.get(ii);
				      String stillblocked = item2.getString("status");	
				      if (!stillblocked.equals("C")) 
				      {
				    	  notblocked = 1;	
				      }
			      }
		    	  if (notblocked == 0)
		    	  {
		    		  // see if anyone waiting on me
		    		  ArrayList<JSONObject> blockedlist3  = dal.GetBlockedServices(blocked_service);
				      for (int ii = 0; ii < blockedlist3.size(); ii++)
				      {
				    	  JSONObject item2 = blockedlist3.get(ii);
					      String stillblockedservice = item2.getString("blocked_service");	
					      String stillblocked = item2.getString("status");	
					      if (stillblocked.equals("W"))
					      {
					    	  // mark this blocked row as complete
					    	  // note marking the row in the blocked list, not the service
					    	  // as complete
					    	  JSONObject blockedrow2 = dal.GetBlockedRow(blocked_service, stillblockedservice);
					    	  blockedrow2.put("status", "C");
							  dal.UpdateBlockedRow(blockedrow2);		  
							  // in complete and final, we can have concurent calls hitting the chain
							  // for example, multiple services completeing the same time
							  // so always to a read after a write in complete and final
							  dal.UpdateBlocked();
					    	  
					    	  sendEvent( rootid, stillblockedservice, true, dal);				
					      }
				      }
		    	  }
		      }
	      }		  
		  DoFinal( jsonrtoos, dal);
		  return;
	}

	// /////////////////////////////////////////////
	//
	//	DoBatch 
	//
	// /////////////////////////////////////////////
	protected String DoBatch( JSONObject jsonrtoos, R2s_DAL dal) throws IOException {
		String sttype = null;
		String strep = null;
		  try 
		  {
		      
			  // get the value
			  sttype = jsonrtoos.getString("service_type");
			  if (sttype.equals("Pre"))
			  {
			      //System.out.println("Pre");
			      strep = DoPre(jsonrtoos, dal);				  
			  }
			  else if (sttype.equals("I") || sttype.equals("C") || sttype.equals("S") || sttype.equals("F")|| sttype.equals("E"))
			  {
			      //System.out.println("New");
			      strep = DoNew(jsonrtoos, dal);				  
			  }

		  } 
		  catch (JSONException e) 
		  {
			  throw new IOException("Error reading request string");
		  }
		  return strep;
	}
	
	protected String DoRetry( JSONObject jsonObject, R2s_DAL dal) throws IOException {
		String rootid = jsonObject.getString("root_service");
		String service = jsonObject.getString("service");
  	    JSONObject row = dal.GetServiceRow(service);    
		String status = row.getString("status");
		if (!status.equals("E")) return "ERROR";	// we only retry errors
		row.put("status", "R");
		dal.UpdateServiceRow(row);		  	      
		sendEvent( rootid, service, false, dal);			// no need for consensus if I am the only one
		return service;
	}	
  /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// jb is the buffer for the json object
		StringBuffer jb = new StringBuffer();
		String line = null;
		try {
			// read the input json into jb
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				jb.append(line);
		} 
		catch (Exception e) { 
			throw new IOException("Error reading request string");
		}

	  	String stinput = jb.toString();

		
		response.getWriter().append("Input at: ").append(stinput);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// jb is the buffer for the json object
		StringBuffer jb = new StringBuffer();
		String line = null;
		String strep = null;
		  try 
		  {
			  // read the input json into jb
			  BufferedReader reader = request.getReader();
			  while ((line = reader.readLine()) != null)
				  jb.append(line);
		  } 
		  catch (Exception e) 
		  { 
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException("Error reading request string");
		  }
	      //System.out.println(jb.toString());
		  /*
		  try 
		  {
			  TimeUnit.SECONDS.sleep(10);	// add a little wait, to see if root will end
		  }
		  catch (JSONException | InterruptedException e) 
		  {
			  throw new IOException(jb.toString());
		  }
*/
	
		  try 
		  {
			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  JSONObject jsonObject =  new JSONObject(jb.toString());
		      String r2type = jsonObject.getString("type");	
		  	  R2s_DAL dal = new R2s_DAL();
		  	  dal.Init();
		      if (r2type.equals("clean")) {	// doesn't need dal.RetrieveServiceTree(rootid); so here up front
		    	  strep = dal.DoClean();
		      }
		      else if (r2type.equals("searchlist")) {	// doesn't need dal.RetrieveServiceTree(rootid); so here up front
			      //System.out.println("searchlist");
		    	  strep = dal.RetrieveSearchList(jsonObject);
		      }
		      else
		      {
				  String rootid = jsonObject.getString("root_service");	// everything after this point needs dal.RetrieveServiceTree(rootid);
		    	  dal.RetrieveServiceTree(rootid);
			      if (r2type.equals("jsontree")) {
				      //System.out.println("jsontree");
			    	  strep = dal.RetrieveJsonTree(rootid);
			      }
			      else if (r2type.equals("retry")) {
				      DoRetry(jsonObject, dal);
			      }
			      else if (r2type.equals("Complete")) {
			    	  
				      JSONObject jsonrtoos = jsonObject.getJSONObject("r2_msg");	
				      DoComplete(jsonrtoos, dal);
			      }
			      else if (r2type.equals("Root")) {
			    	  
				      JSONObject jsonrtoos = jsonObject.getJSONObject("r2_msg");	
				      strep = DoRoot(jsonrtoos, dal);
			      }
			      else if (r2type.equals("Clean")) {
			    	  
				      strep = dal.DoClean();
			      }
			      else if (r2type.equals("Batch")) {
				      String serviceid = jsonObject.getString("service");

			    	  JSONArray regarray = 	jsonObject.getJSONArray("r2_msg");	
			    	  
			    	  for (int i = 0; i < regarray.length(); i++) {
			    		  JSONObject jsonrtoos = regarray.getJSONObject(i);
					      //System.out.println(jsonrtoos);

					      strep += DoBatch(jsonrtoos, dal) + " ";
			    	  }
			    	  
			    	  // have to do after batch as "Pre" might change things
			    	  ArrayList<JSONObject> all2 = dal.GetServiceChildren(serviceid);
				      for (int i = 0; i < all2.size(); i++)
				      {
					      String servicetype = all2.get(i).getString("servicetype");	
					      String neweventid = all2.get(i).getString("service");	
					      if (servicetype.equals("I"))	// kick off independent events
					      {
					    	  sendEvent( rootid,    neweventid, false, dal);		
					      }
					      else if (servicetype.equals("C"))	// kick off contained events
					      {
					    	  sendEvent( rootid,    neweventid, false, dal);		
					      }				      
				      }
			      }


	/* trash section to see if we can iterate through it all		    	  
			    	  Map<String, JSONObject> all = dal.GetServiceIDtoRow();
			    	  all.forEach((id, row) ->
			    	  {
			    		  try {
						      String status = row.getString("status");	
						      String neweventid = row.getString("service");	
						      if (status.equals("I"))	// kick off independent events
						      {
						    	  sendEvent(rootid, neweventid);		
						      }
						      else if (status.equals("C"))	// kick off contained events
						      {
						    	  sendEvent(rootid, neweventid );		
						      }				      
			    		  }
						  catch (IOException e) 
						  {
							  throw new JSONException(jb.toString());
						  }
			    	  });
	*/
		    	  
	    	  }

		      dal.CleanUp(); 
		  }
		  catch (JSONException e) 
		  {
			  throw new IOException(jb.toString());
		  }
		//System.out.println(strep);
		response.getWriter().append(strep);
		response.flushBuffer();
		
		
	}

}
