package r2fileapp;

import java.io.BufferedReader;
import java.util.concurrent.*;
import java.io.IOException;

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
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
import java.util.UUID;



/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class R2 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public Cluster cluster2;
	//public Session session;
	public static Semaphore mysemaphore;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public R2() {
        super();
    }

    public void init(ServletConfig config) throws ServletException {
   	  	cluster2 = Cluster.builder().addContactPoint("127.0.0.1").build();
   	  	
   	  	mysemaphore = new Semaphore(2000);	// must be > 1
    }
    
    public void destroy() {
    	
    	cluster2.close();	// not sure this does anything
   }
    
	protected void sendEvent(String rootid, String serviceid, R2_DAL dal) throws IOException {
		
  	  JSONObject row = dal.GetServiceRow(serviceid);

		if (dal.UpdateSendStatus(row))
		{
			
		  String newurl = row.getString("service_url");
		  String newparam = row.getString("service_param");
		  String newparent = row.getString("parent_service");
		  
		  try {
			  mysemaphore.acquire();
		      R2SendThread T1 = new R2SendThread( rootid, newparent, serviceid, newurl, newparam, mysemaphore);
				  
		      T1.start();
		  }
		  catch (Exception e) { 
				throw new IOException("Error starting thread");
		  }   	  
		}
		else
			System.out.println("OY");	  
}

	
	// /////////////////////////////////////////////
	//
	//	DoRoot
	//
	// /////////////////////////////////////////////
	protected String DoRoot ( JSONObject jsonrtoos, R2_DAL dal) throws IOException {
		
	  JSONObject newobj = new JSONObject();		
	  String eventid = jsonrtoos.getString("service");
	  newobj.put("root_service", eventid);
	  newobj.put("parent_service", eventid);
	  newobj.put("service", eventid);
	  String serviceurl = jsonrtoos.getString("service_url");
	  newobj.put("service_url", serviceurl);
	  String serviceparam = jsonrtoos.getString("service_param");
	  newobj.put("service_param", serviceparam);
	  newobj.put("status", "R");
	  newobj.put("servicetype", "R");
	  
	  dal.UpdateServiceRow(newobj);
		
      sendEvent( eventid,  eventid, dal);			      
      return  eventid;
	}

	// /////////////////////////////////////////////
	//
	//	DoPre
	//
	// /////////////////////////////////////////////
	protected String DoPre ( JSONObject jsonrtoos, R2_DAL dal) throws IOException {
		  Session session2 = cluster2.connect();
		  session2.execute("USE rtoos");

		  String rootid = jsonrtoos.getString("root_service");
		  String preid = jsonrtoos.getString("pre_service");
		  String eventid = jsonrtoos.getString("blocked_service");

		  session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?) IF NOT EXISTS;", 
			  UUID.fromString(rootid), UUID.fromString(preid), UUID.fromString(eventid), "W");

    	  JSONObject row = dal.GetServiceRow(eventid);
		  
		  row.put("servicetype", "S");
		  dal.UpdateServiceRow(row);

		  return eventid.toString();
	}

	
	// /////////////////////////////////////////////
	//
	//	DoNew
	//
	// /////////////////////////////////////////////
	protected String DoNew ( JSONObject jsonrtoos, R2_DAL dal) throws IOException {
		
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
		  
		  dal.UpdateServiceRow(newobj);
		  
		  Session session2 = cluster2.connect();
 	  	  session2.execute("USE rtoos");

	      if (servicetype.equals("S"))	// successor, add eventid to blocked table as waiting (waiting for parent)
	      {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?) IF NOT EXISTS;", 
		    		  UUID.fromString(rootid), UUID.fromString(partentid), UUID.fromString(eventid), "W");
	      }
	      else if (servicetype.equals("C"))	// contained, add parent to blocked table (blocked by eventid)
	      {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?) IF NOT EXISTS;", 
		    		  UUID.fromString(rootid), UUID.fromString(eventid), UUID.fromString(partentid),  "B");
	    	  //sendEvent( rootid,  eventid , dal);		
	      }				      
	      else if (servicetype.equals("F"))	// contained, add parent to blocked table (blocked by eventid)
	      {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?) IF NOT EXISTS;", 
		    		  UUID.fromString(rootid), UUID.fromString(partentid), UUID.fromString(eventid), "F");
	      }				      
	      //else if (servicetype.equals("I"))	// Independent, just run
	      //{
	    	  //sendEvent( rootid, eventid, dal);					    	  
	      //}
	      session2.close();
		  return eventid;
	}
	

	// /////////////////////////////////////////////
	//
	//	DoFinal
	//
	// /////////////////////////////////////////////
	protected void DoFinal ( JSONObject jsonrtoos, R2_DAL dal) throws IOException {
		  Session session2 = cluster2.connect();
 	  	  session2.execute("USE rtoos");

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
				      if (!status.equals("F") && !service.equals(rootid) && !servicetype.equals("F"))	// got a status that is not finished
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
			  dal.UpdateServices();	// when doing complete, AFTER any updates we need to refresh to avoid race condition		
				  			      
		      // see if service is set to run on finished
		      String stquery3 = "SELECT *  FROM blocked_list WHERE ";
		      stquery3 += "root_service = ";
		      stquery3 += rootid;
		      stquery3 += " AND pre_service = ";
		      stquery3 += testservice;
		      //System.out.println(stquery);
		      ResultSet resultSet3 = session2.execute(stquery3);
		      List<Row> all3 = resultSet3.all();
		      //int gotone = 0;
		      for (int i = 0; i < all3.size(); i++)
		      {
		    	  String status = all3.get(i).getString("status");
		    	  if (status.equals("F"))
		    	  {
						//System.out.println("RUNNING A FINAL");
						
						// and update blocked list
						String blockedservice = all3.get(i).getUUID("blocked_service").toString();	
						String stquery4 = "UPDATE blocked_list SET status  = 'C'";
						stquery4 += " WHERE";
						stquery4 += " root_service = ";
						stquery4 += rootid;
						stquery4 += " AND pre_service = ";
						stquery4 += testservice;
						stquery4 += " AND blocked_service = ";
						stquery4 += blockedservice;
						// System.out.println(stquery);
						session2.execute(stquery4);
						
		    		    // run it
						sendEvent( rootid,    blockedservice, dal);
						
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

		  session2.close();  
}
	
	// /////////////////////////////////////////////
	//
	//	DoComplete
	//
	// /////////////////////////////////////////////
	protected void DoComplete ( JSONObject jsonrtoos, R2_DAL dal) throws IOException {

		  String rootid = jsonrtoos.getString("root_service");
		  String partentid = jsonrtoos.getString("parent_service");
		  String eventid = jsonrtoos.getString("service");
		  
    	  JSONObject row = dal.GetServiceRow(eventid);
		  
		  row.put("status", "C");
		  dal.UpdateServiceRow(row);		  	      
		  dal.UpdateServices();	// when doing complete, AFTER any updates we need to refresh to avoid race condition		

    	  Session session2 = cluster2.connect();
 	  	  session2.execute("USE rtoos");
	      // first see if this is waiting on any (contained "B") to finish
 	  	  // this would be the contained case
 	  	  // the easy case
	      String stquery = "SELECT *  FROM blocked_list WHERE ";
	      stquery += "root_service = ";
	      stquery += rootid;
	      stquery += " AND blocked_service = ";
	      stquery += eventid;
	      //System.out.println(stquery);
	      ResultSet resultSet = session2.execute(stquery);
	      int stillwaiting = 0;
	      List<Row> all = resultSet.all();
	      for (int i = 0; i < all.size(); i++)
	      {
		      String mystatus = all.get(i).getString("status");	
		      if (mystatus.equals("B"))
		      {
		    	  stillwaiting = 1;					    	  
		      }
	      }
	      if (stillwaiting == 1) 
	      {
			  DoFinal( jsonrtoos, dal);

	    	  return;
	      }
	      
	      // see if anyone waiting on this
	      stquery = "SELECT *  FROM blocked_list WHERE ";
	      stquery += "root_service = ";
	      stquery += rootid;
	      stquery += " AND pre_service = ";
	      stquery += eventid;
	      //System.out.println(stquery);
	      resultSet = session2.execute(stquery);
	      
	      all = resultSet.all();
	      for (int i = 0; i < all.size(); i++)
	      {
		      String blocked_service = all.get(i).getUUID("blocked_service").toString();
		      String blocked_status = all.get(i).getString("status");	

	    	  					      					      
		      if(blocked_status.equals("W"))
		      {
		    	  // first update the status
				  stquery = "UPDATE blocked_list SET status  = 'C'";
				  stquery += " WHERE";
			      stquery += " root_service = ";
			      stquery += rootid;
			      stquery += " AND pre_service = ";
			      stquery += eventid;
			      stquery += " AND blocked_service = ";
			      stquery += blocked_service;
			      // System.out.println(stquery);
			      session2.execute(stquery);

			      // aways do another read after status change
			      // see if still blocked
			      stquery = "SELECT *  FROM blocked_list WHERE ";
			      stquery += "root_service = ";
			      stquery += rootid;
			      stquery += " AND blocked_service = ";
			      stquery += blocked_service;
			      //System.out.println(stquery);
			      ResultSet resultSet2 = session2.execute(stquery);
			      List<Row> all2 = resultSet2.all();
			      int notblocked = 0;
			      // for wait, really should only have 1 record
			      // but we will loop
			      for (int ii = 0; ii < all2.size(); ii++)
			      {
				      String stillblocked = all2.get(ii).getString("status");	
				      if (!stillblocked.equals("C")) //$$$
				      {
				    	  notblocked = 1;	
				      }
			    	  
			      }
		    	  if (notblocked == 0)// simple case, parent finished, can release
		    	  {
			    	  sendEvent( rootid,    blocked_service, dal);										    	  
		    	  }				    	  
		      }
		      else if (blocked_status.equals("B")) // 
		      {
		    	  // this one is blocked, at least marked the blocked list as C
				  stquery = "UPDATE blocked_list SET status  = 'C'";
				  stquery += " WHERE";
			      stquery += " root_service = ";
			      stquery += rootid;
			      stquery += " AND pre_service = ";
			      stquery += eventid;
			      stquery += " AND blocked_service = ";
			      stquery += blocked_service;
			      // System.out.println(stquery);
			      session2.execute(stquery);

			      // see if still blocked
			      stquery = "SELECT *  FROM blocked_list WHERE ";
			      stquery += "root_service = ";
			      stquery += rootid;
			      stquery += " AND blocked_service = ";
			      stquery += blocked_service;
			      //System.out.println(stquery);
			      ResultSet resultSet2 = session2.execute(stquery);
			      List<Row> all2 = resultSet2.all();
			      int notblocked = 0;
			      for (int ii = 0; ii < all2.size(); ii++)
			      {
				      String stillblocked = all2.get(ii).getString("status");	
				      if (!stillblocked.equals("C")) //$$$
				      {
				    	  notblocked = 1;					    	  
				      }
			    	  
			      }
		    	  if (notblocked == 0)
		    	  {
		    		  // see if anyone waiting on me
				      stquery = "SELECT *  FROM blocked_list WHERE ";
				      stquery += "root_service = ";
				      stquery += rootid;
				      stquery += " AND pre_service = ";
				      stquery += blocked_service;
				      //System.out.println(stquery);
				      ResultSet resultSet3 = session2.execute(stquery);
				      List<Row> all3 = resultSet3.all();
				      for (int iii = 0; iii < all3.size(); iii++)
				      {
				    	  Row blockedlistrow = all3.get(iii);
				    	  
					      String stillblockedservice = blockedlistrow.getUUID("blocked_service").toString();	
					      String stillblocked = blockedlistrow.getString("status");	
					      if (stillblocked.equals("W"))
					      {
					    	  sendEvent( rootid,    stillblockedservice, dal);				
					    	  
							  stquery = "UPDATE blocked_list SET status  = 'C'";
							  stquery += " WHERE";
						      stquery += " root_service = ";
						      stquery += rootid;
						      stquery += " AND pre_service = ";
						      stquery += blocked_service;
						      stquery += " AND blocked_service = ";
						      stquery += stillblockedservice;
						      //System.out.println(stquery);
						      session2.execute(stquery);
					      }						    	  
				      }
		    	  }
		    	  
		      }
	      }
		  session2.close();  
		  
		  DoFinal( jsonrtoos, dal);
		  return;
	}

	// /////////////////////////////////////////////
	//
	//	DoBatch 
	//
	// /////////////////////////////////////////////
	protected String DoBatch( JSONObject jsonrtoos, R2_DAL dal) throws IOException {
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
			  else if (sttype.equals("I") || sttype.equals("C") || sttype.equals("S") || sttype.equals("F"))
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
			/*report an error*/ 
			// crash and burn
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
			  String rootid = jsonObject.getString("root_service");
	    	  R2_DAL dal = new R2_DAL(cluster2);
	    	  dal.RetrieveServiceTree(rootid);
		      if (r2type.equals("Complete")) {
		    	  
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
				    	  sendEvent( rootid,    neweventid, dal);		
				      }
				      else if (servicetype.equals("C"))	// kick off contained events
				      {
				    	  sendEvent( rootid,    neweventid, dal);		
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

		  }
		  catch (JSONException e) 
		  {
			  throw new IOException(jb.toString());
		  }
		  
		response.getWriter().append(strep);
		response.flushBuffer();
		
		
	}

}
