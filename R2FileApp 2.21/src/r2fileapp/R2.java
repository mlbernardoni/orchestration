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

import java.util.List;
import java.util.UUID;



/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class R2 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public Cluster cluster2;
	//public Session session;
	public Semaphore mysemaphore;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public R2() {
        super();
    }

    public void init(ServletConfig config) throws ServletException {
    	//System.out.println("From Init Method");
   	  	cluster2 = Cluster.builder().addContactPoint("127.0.0.1").build();
   	  	//session = cluster.connect();
   	  	//session.execute("USE rtoos");
   	  	
   	  	mysemaphore = new Semaphore(2000);	// must be > 1
    }
    
    public void destroy() {
    	
    	//System.out.println("From destroy Method");
    	//session.close();
    	cluster2.close();	// not sure this does anything
   }
    
	protected void sendEvent(String rootid, String serviceid) throws IOException {
//   	  	  Cluster cluster2 = Cluster.builder().addContactPoint("127.0.0.1").build();
 		  Session session2 = cluster2.connect();
   	  	  session2.execute("USE rtoos");
	      String stquery = "SELECT service_url, service_param, parent_service FROM service_tree WHERE ";
	      stquery += "root_service = ";
	      stquery += rootid;
	      //stquery += " AND parent_service = ";
	      //stquery += partentid;
	      stquery += " AND service = ";
	      stquery += serviceid;
	      //System.out.println(stquery);
	      ResultSet resultSet = session2.execute(stquery);
	      
	      List<Row> all = resultSet.all();
	      String newurl = all.get(0).getString("service_url");
	      String newparam = all.get(0).getString("service_param");
	      String newparent = all.get(0).getUUID("parent_service").toString();
	      
		  try {
			  mysemaphore.acquire();
		      R2SendThread T1 = new R2SendThread( rootid, newparent, serviceid, newurl, newparam, mysemaphore);
	
		      stquery = "UPDATE service_tree SET status  = 'P' WHERE ";
		      stquery += "root_service = ";
		      stquery += rootid;
		      stquery += " AND parent_service = ";
		      stquery += newparent;
		      stquery += " AND service = ";
		      stquery += serviceid;
		      //System.out.println(stquery);
		      session2.execute(stquery);
			  // System.out.println(resp.toString());
			  
		      T1.start();
		  }
		  catch (Exception e) { 
				/*report an error*/ 
				// crash and burn
				throw new IOException("Error starting thread");
		  }
		  session2.close();
//		  cluster2.close();
	}

	// /////////////////////////////////////////////
	//
	//	DoClean
	//
	// /////////////////////////////////////////////
	protected String DoClean () throws IOException {
		
	  Session session2 = cluster2.connect();
  	  session2.execute("USE rtoos");
      session2.execute("TRUNCATE service_tree;");
      session2.execute("TRUNCATE blocked_list;");
      session2.close();
      return  "Cleaned";
	}
	
	// /////////////////////////////////////////////
	//
	//	DoRoot
	//
	// /////////////////////////////////////////////
	protected String DoRoot ( JSONObject jsonrtoos) throws IOException {
	  Session session2 = cluster2.connect();
  	  session2.execute("USE rtoos");
      //System.out.println("got root");
      //session.execute("CREATE KEYSPACE IF NOT EXISTS rtoos WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;");
      //session.execute("CREATE TABLE IF NOT EXISTS rtoos.service_tree (root_service UUID,parent_service UUID,service UUID,service_url text,service_param text,PRIMARY KEY(root_service, parent_service, service));");

      UUID eventid = UUID.fromString(jsonrtoos.getString("service"));
	  String serviceurl = jsonrtoos.getString("service_url");
	  String serviceparam = jsonrtoos.getString("service_param");
      session2.execute("INSERT INTO service_tree (root_service, parent_service, service, service_url, service_param, status) VALUES (?, ?, ?, ?, ?, ?);", 
    		  eventid, eventid, eventid, serviceurl, serviceparam, "R");
      //System.out.println("wrote cassandra");
      session2.close();
      //System.out.println("wrote cassandra");
      sendEvent( eventid.toString(),  eventid.toString());			      
      return  eventid.toString();
	}
	
	// /////////////////////////////////////////////
	//
	//	DoRegister
	//
	// /////////////////////////////////////////////
	protected String DoRegister ( JSONObject jsonrtoos) throws IOException {
		  Session session2 = cluster2.connect();
		  session2.execute("USE rtoos");
		  String rootid = jsonrtoos.getString("root_service");
		  String partentid = jsonrtoos.getString("parent_service");
		  String serviceurl = jsonrtoos.getString("service_url");
		  String serviceparam = jsonrtoos.getString("service_param");
		  String servicetype = jsonrtoos.getString("service_type");
		  UUID eventid = UUID.fromString(jsonrtoos.getString("service"));
		  session2.execute("INSERT INTO service_tree (root_service, parent_service, service, service_url, service_param, status) VALUES (?, ?, ?, ?, ?, ?);", 
				  UUID.fromString(rootid), UUID.fromString(partentid), eventid, serviceurl, serviceparam, servicetype);
		  
		  if (servicetype.equals("S"))	// successor, add eventid to blocked table as waiting (waiting for parent)
		  {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
		    		  UUID.fromString(rootid), UUID.fromString(partentid), (eventid), "W");
		  }
		  else if (servicetype.equals("C"))	// contained, add parent to blocked table (blocked by eventid)
		  {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
		    		  UUID.fromString(rootid), (eventid), UUID.fromString(partentid), "B");
		  }		
		  else if (servicetype.equals("F"))	// contained, add parent to blocked table (blocked by eventid)
		  {
			  //System.out.println("OY1");
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
		    		  UUID.fromString(rootid), UUID.fromString(partentid), (eventid), "F");
		  }		
		  session2.close();
		  return eventid.toString();
	}

	// /////////////////////////////////////////////
	//
	//	DoPre
	//
	// /////////////////////////////////////////////
	protected String DoPre ( JSONObject jsonrtoos) throws IOException {
		  Session session2 = cluster2.connect();
		  session2.execute("USE rtoos");
		  //System.out.println("OY1");
		  String rootid = jsonrtoos.getString("root_service");
		  String preid = jsonrtoos.getString("pre_service");
		  String eventid = jsonrtoos.getString("blocked_service");
		  //System.out.println(rootid);
		  //System.out.println(preid);
		  //System.out.println(eventid);
		  session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
			  UUID.fromString(rootid), UUID.fromString(preid), UUID.fromString(eventid), "W");
		
		  String stquery = "SELECT parent_service  FROM service_tree WHERE ";
		  stquery += "root_service = ";
		  stquery += rootid;
		  stquery += " AND service = ";
		  stquery += eventid;
		  //System.out.println(stquery);
		  ResultSet resultSet = session2.execute(stquery);
		  List<Row> all = resultSet.all();
		  String theparent = all.get(0).getUUID("parent_service").toString();
		  //System.out.println("OY2");
		  //System.out.println(theparent);
		  
		  
		  stquery = "UPDATE service_tree SET status  = 'S' WHERE root_service = ";
		  stquery += rootid;
		  stquery += " AND parent_service = ";
		  stquery += theparent;
		  stquery += " AND service = ";
		  stquery += eventid;
		  //System.out.println(stquery);
		  session2.execute(stquery);
		  session2.close();
		  return eventid.toString();
	}

	// /////////////////////////////////////////////
	//
	//	DoNew
	//
	// /////////////////////////////////////////////
	protected String DoNew ( JSONObject jsonrtoos) throws IOException {
		  Session session2 = cluster2.connect();
 	  	  session2.execute("USE rtoos");
	      //System.out.println("got new");
		  String rootid = jsonrtoos.getString("root_service");
		  String partentid = jsonrtoos.getString("parent_service");
		  String serviceurl = jsonrtoos.getString("service_url");
		  String serviceparam = jsonrtoos.getString("service_param");
		  String servicetype = jsonrtoos.getString("service_type");
	      UUID eventid = UUID.fromString(jsonrtoos.getString("service"));
	      session2.execute("INSERT INTO service_tree (root_service, parent_service, service, service_url, service_param, status) VALUES (?, ?, ?, ?, ?, ?);", 
	    		  UUID.fromString(rootid), UUID.fromString(partentid), eventid, serviceurl, serviceparam, servicetype);
	      
	      if (servicetype.equals("S"))	// successor, add eventid to blocked table as waiting (waiting for parent)
	      {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
		    		  UUID.fromString(rootid), UUID.fromString(partentid), (eventid), "W");
	      }
	      else if (servicetype.equals("C"))	// contained, add parent to blocked table (blocked by eventid)
	      {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
		    		  UUID.fromString(rootid), (eventid), UUID.fromString(partentid),  "B");
	    	  sendEvent( rootid,    eventid.toString() );		
	      }				      
	      else if (servicetype.equals("F"))	// contained, add parent to blocked table (blocked by eventid)
	      {
		      session2.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
		    		  UUID.fromString(rootid), UUID.fromString(partentid), (eventid), "F");
	      }				      
	      else if (servicetype.equals("I"))	// Independent, just run
	      {
	    	  sendEvent( rootid,    eventid.toString() );					    	  
	      }
	      session2.close();
		  return eventid.toString();
	}

	// /////////////////////////////////////////////
	//
	//	DoFinal
	//
	// /////////////////////////////////////////////
	protected void DoFinal ( JSONObject jsonrtoos) throws IOException {
//System.out.println("DoFinal 1");
		  Session session2 = cluster2.connect();
 	  	  session2.execute("USE rtoos");

 	  	  String rootid = jsonrtoos.getString("root_service");
		  String partentid = jsonrtoos.getString("parent_service");
		  String eventid = jsonrtoos.getString("service");
		  
		  String testparent = partentid;
//System.out.println(testparent);
		  String testservice = eventid;
//System.out.println(testservice);
		  while (testservice != "")
		  {
//System.out.println(testservice);
			  // see if any not finished
		      String stquery = "SELECT status, service  FROM service_tree WHERE ";
		      stquery += "root_service = ";
		      stquery += rootid;	
		      stquery += " AND parent_service = ";
		      stquery += testservice;
		      //System.out.println(stquery);
		      ResultSet resultSet = session2.execute(stquery);
		      List<Row> all = resultSet.all();
		      int notfinished = 0;
		      for (int i = 0; i < all.size(); i++)
		      {
			      String status = all.get(i).getString("status");	
			      String service = all.get(i).getUUID("service").toString();
			      if (!status.equals("F") && !service.equals(rootid))	// got a status that is not finished
			    	  notfinished = 1;
		      }	
		      // this one is finished
		      if (notfinished == 0 )
		      {
				  String stquery2 = "UPDATE service_tree SET status  = 'F' WHERE root_service = ";
			      stquery2 += rootid;
			      stquery2 += " AND parent_service = ";
			      stquery2 += testparent;
			      stquery2 += " AND service = ";
			      stquery2 += testservice;
			      //System.out.println(stquery);
			      session2.execute(stquery2);
			      
			      // see if service is set to run on finished
			      String stquery3 = "SELECT *  FROM blocked_list WHERE ";
			      stquery3 += "root_service = ";
			      stquery3 += rootid;
			      stquery3 += " AND pre_service = ";
			      stquery3 += testservice;
			      //System.out.println(stquery);
			      ResultSet resultSet3 = session2.execute(stquery3);
			      List<Row> all3 = resultSet3.all();
			      int gotone = 0;
			      for (int i = 0; i < all3.size(); i++)
			      {
			    	  String status = all3.get(i).getString("status");
			    	  if (status.equals("F"))
			    	  {
							String blockedservice = all3.get(i).getUUID("blocked_service").toString();	
							sendEvent( rootid,    blockedservice);
							
							System.out.println("RUNNING A FINAL");
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
			    	  }
			      }
		      }
		      
		      if (testservice.equals(rootid))
		      {
		    	  testservice = "";
		      }
		      else
		      {
//System.out.println("DoFinal 2");
//System.out.println(testservice);
//System.out.println(testparent);
		    	  testservice = testparent;

		    	  String stquery2 = "SELECT parent_service FROM service_tree WHERE ";
			      stquery2 += "root_service = ";
			      stquery2 += rootid;
			      stquery2 += " AND service = ";
			      stquery2 += testservice;
			      //System.out.println(stquery2);
			      ResultSet resultSet2 = session2.execute(stquery2);
			      List<Row> all2 = resultSet2.all();
			      testparent = all2.get(0).getUUID("parent_service").toString();
//System.out.println(testparent);
		      }
		  }

		  session2.close();  
//System.out.println("DoFinal ending");
}
	
	// /////////////////////////////////////////////
	//
	//	DoComplete
	//
	// /////////////////////////////////////////////
	protected void DoComplete ( JSONObject jsonrtoos) throws IOException {
//	   	  Cluster cluster2 = Cluster.builder().addContactPoint("127.0.0.1").build();
		  Session session2 = cluster2.connect();
 	  	  session2.execute("USE rtoos");
		  String rootid = jsonrtoos.getString("root_service");
		  String partentid = jsonrtoos.getString("parent_service");
		  String eventid = jsonrtoos.getString("service");

		  String stquery = "UPDATE service_tree SET status  = 'C' WHERE root_service = ";
	      stquery += rootid;
	      stquery += " AND parent_service = ";
	      stquery += partentid;
	      stquery += " AND service = ";
	      stquery += eventid;
	      //System.out.println(stquery);
	      session2.execute(stquery);
	      
	      // first see if this is waiting on any (contained "B") to finish
	      stquery = "SELECT *  FROM blocked_list WHERE ";
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
			  DoFinal( jsonrtoos);

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
			    	  sendEvent( rootid,    blocked_service);										    	  
		    	  }				    	  
		      }
		      else if (blocked_status.equals("B")) // 
		      {
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
					      String stillblockedservice = all3.get(iii).getUUID("blocked_service").toString();	
					      String stillblocked = all3.get(iii).getString("status");	
					      if (stillblocked.equals("W"))
					      {
					    	  sendEvent( rootid,    stillblockedservice);				
					    	  
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
		  
		  DoFinal( jsonrtoos);
		  return;
	}

	// /////////////////////////////////////////////
	//
	//	DoInput
	//
	// /////////////////////////////////////////////
	protected String DoInput( JSONObject jsonrtoos) throws IOException {
		String sttype = null;
		String strep = null;
		  try 
		  {
		      
			  // get the value
			  sttype = jsonrtoos.getString("type");
		   	  //cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		   	  //session = cluster.connect();
		   	  //session.execute("USE rtoos");
			  if (sttype.equals("Root"))
			  {
			      strep = DoRoot(jsonrtoos);
			  }
			  else if (sttype.equals("Clean"))
			  {
				  strep = DoClean();
			  }
			  else if (sttype.equals("Register"))
			  {
			      strep = DoRegister(jsonrtoos);
			  }
			  else if (sttype.equals("Pre"))
			  {
			      strep = DoPre(jsonrtoos);				  
			  }
			  else if (sttype.equals("New"))
			  {
			      strep = DoNew(jsonrtoos);				  
			  }
			  else if (sttype.equals("Update"))
			  {
				  String status = jsonrtoos.getString("status");

				  if (status.equals("Complete"))	// Complete
			      {
				      DoComplete(jsonrtoos);				  
			      }
			      
			  }

		  } 
		  catch (JSONException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
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
		// TODO Auto-generated method stub
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
	
		  try 
		  {
			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  JSONObject jsonObject =  new JSONObject(jb.toString());
		      String r2type = jsonObject.getString("type");	
		      if (r2type.equals("Indvidual")) {
		    	  
			      JSONObject jsonrtoos = jsonObject.getJSONObject("rtoos_msg");	
			      strep = DoInput(jsonrtoos);
		      }
		      else if (r2type.equals("Batch")) {
//			   	  Cluster cluster2 = Cluster.builder().addContactPoint("127.0.0.1").build();
				  Session session2 = cluster2.connect();
		   	  	  session2.execute("USE rtoos");
		    	  JSONArray regarray = 	jsonObject.getJSONArray("rtoos_msg");	
		    	  
		    	  for (int i = 0; i < regarray.length(); i++) {
		    		  JSONObject jsonrtoos = regarray.getJSONObject(i);
				      //System.out.println(jsonrtoos);

				      strep += DoInput(jsonrtoos) + " ";
		    	  }		      
			      String rootid = jsonObject.getString("root_service");
			      String serviceid = jsonObject.getString("service");
			      String stquery = "SELECT *  FROM  service_tree WHERE ";
			      stquery += "root_service = ";
			      stquery += rootid;	
			      stquery += " AND parent_service = ";
			      stquery += serviceid;
			      //System.out.println(stquery);
			      ResultSet resultSet = session2.execute(stquery);
			      List<Row> all = resultSet.all();
			      for (int i = 0; i < all.size(); i++)
			      {
				      String status = all.get(i).getString("status");	
				      String neweventid = all.get(i).getUUID("service").toString();	
				      if (status.equals("I"))	// kick off independent events
				      {
				    	  sendEvent( rootid,    neweventid);		
				      }
				      else if (status.equals("C"))	// kick off contained events
				      {
				    	  sendEvent( rootid,    neweventid );		
				      }				      
			      }
			      session2.close();
//			      cluster2.close();
	    	  }

		  }
		  catch (JSONException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }
		  
		response.getWriter().append(strep);
		response.flushBuffer();
		
		
	}

}
