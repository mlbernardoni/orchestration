package rtoos;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;

import java.util.List;
import java.util.UUID;



/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class RTOOS extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public Cluster cluster;
	public Session session;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RTOOS() {
        super();
    }

    public void init(ServletConfig config) throws ServletException {
    	//System.out.println("From Init Method");
   	  	cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
   	  	session = cluster.connect();
   	  	session.execute("USE rtoos");
    }
    
    public void destroy() {
    	
    	//System.out.println("From destroy Method");
    	cluster.close();	// not sure this does anything
   }
    
	protected void sendEvent(String rootid, String partentid, String eventid) throws IOException {
	      String stquery = "SELECT service_url, service_param FROM service_tree WHERE ";
	      stquery += "root_service = ";
	      stquery += rootid;
	      stquery += " AND parent_service = ";
	      stquery += partentid;
	      stquery += " AND service = ";
	      stquery += eventid.toString();
	      //System.out.println(stquery);
	      ResultSet resultSet = session.execute(stquery);
	      
	      List<Row> all = resultSet.all();
	      String newurl = all.get(0).getString("service_url");
	      String newparam = all.get(0).getString("service_param");
	      
	      SendThread T1 = new SendThread( rootid, partentid, eventid, newurl, newparam);
	      T1.start();

	      stquery = "UPDATE service_tree SET status  = 'P' WHERE ";
	      stquery += "root_service = ";
	      stquery += rootid;
	      stquery += " AND parent_service = ";
	      stquery += partentid;
	      stquery += " AND service = ";
	      stquery += eventid.toString();
	      //System.out.println(stquery);
	      resultSet = session.execute(stquery);
		  // System.out.println(resp.toString());
	      
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
		String sttype = null;
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
		      JSONObject jsonrtoos = (JSONObject)jsonObject.get("rtoos_msg");	  
			  // get the value
			  sttype = jsonrtoos.getString("type");
		   	  //cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		   	  //session = cluster.connect();
		   	  //session.execute("USE rtoos");
			  if (sttype.equals("Root"))
			  {
			      //System.out.println("got root");
			      //session.execute("CREATE KEYSPACE IF NOT EXISTS rtoos WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;");
			      //session.execute("CREATE TABLE IF NOT EXISTS rtoos.service_tree (root_service UUID,parent_service UUID,service UUID,service_url text,service_param text,PRIMARY KEY(root_service, parent_service, service));");

			      UUID eventid = UUIDs.timeBased();
				  String serviceurl = jsonrtoos.getString("service_url");
				  String serviceparam = jsonrtoos.getString("service_param");
			      session.execute("INSERT INTO service_tree (root_service, parent_service, service, service_url, service_param, status) VALUES (?, ?, ?, ?, ?, ?);", 
			    		  eventid, eventid, eventid, serviceurl, serviceparam, "R");
			      strep = eventid.toString();
			      //System.out.println("wrote cassandra");

			      sendEvent( eventid.toString(),  eventid.toString(),  eventid.toString());			      
			  }
			  else if (sttype.equals("Clean"))
			  {
			      session.execute("TRUNCATE service_tree;");
			      session.execute("TRUNCATE blocked_list;");
			      strep = "Cleaned";
			  }
			  else if (sttype.equals("Register"))
			  {
			      //System.out.println("got new");
				  String rootid = jsonrtoos.getString("root_service");
				  String partentid = jsonrtoos.getString("parent_service");
				  String serviceurl = jsonrtoos.getString("service_url");
				  String serviceparam = jsonrtoos.getString("service_param");
				  String servicetype = jsonrtoos.getString("service_type");
			      UUID eventid = UUIDs.timeBased();
			      session.execute("INSERT INTO service_tree (root_service, parent_service, service, service_url, service_param, status) VALUES (?, ?, ?, ?, ?, ?);", 
			    		  UUID.fromString(rootid), UUID.fromString(partentid), eventid, serviceurl, serviceparam, servicetype);
			      strep = eventid.toString();
			      
			      if (servicetype.equals("S"))	// successor, add eventid to blocked table as waiting (waiting for parent)
			      {
				      session.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
				    		  UUID.fromString(rootid), UUID.fromString(partentid), (eventid), "W");
			      }
			      else if (servicetype.equals("C"))	// contained, add parent to blocked table (blocked by eventid)
			      {
					  String blockedparent = jsonrtoos.getString("blocked_parent");
				      session.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, blocked_parent, status) VALUES (?, ?, ?, ?, ?);", 
				    		  UUID.fromString(rootid), (eventid), UUID.fromString(partentid), UUID.fromString(blockedparent), "B");
			      }				      
	
			  }
			  else if (sttype.equals("Pre"))
			  {
				  
				  String rootid = jsonrtoos.getString("root_service");
				  String preid = jsonrtoos.getString("pre_service");
				  String eventid = jsonrtoos.getString("blocked_service");
			      session.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
			    		  UUID.fromString(rootid), UUID.fromString(preid), (eventid), "W");
			      strep = eventid;
			  }
			  else if (sttype.equals("New"))
			  {
			      //System.out.println("got new");
				  String rootid = jsonrtoos.getString("root_service");
				  String partentid = jsonrtoos.getString("parent_service");
				  String serviceurl = jsonrtoos.getString("service_url");
				  String serviceparam = jsonrtoos.getString("service_param");
				  String servicetype = jsonrtoos.getString("service_type");
			      UUID eventid = UUIDs.timeBased();
			      session.execute("INSERT INTO service_tree (root_service, parent_service, service, service_url, service_param, status) VALUES (?, ?, ?, ?, ?, ?);", 
			    		  UUID.fromString(rootid), UUID.fromString(partentid), eventid, serviceurl, serviceparam, servicetype);
			      strep = eventid.toString();
			      
			      if (servicetype.equals("S"))	// successor, add eventid to blocked table as waiting (waiting for parent)
			      {
				      session.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, status) VALUES (?, ?, ?, ?);", 
				    		  UUID.fromString(rootid), UUID.fromString(partentid), (eventid), "W");
			      }
			      else if (servicetype.equals("C"))	// contained, add parent to blocked table (blocked by eventid)
			      {
					  String blockedparent = jsonrtoos.getString("blocked_parent");
				      session.execute("INSERT INTO blocked_list (root_service, pre_service, blocked_service, blocked_parent, status) VALUES (?, ?, ?, ?, ?);", 
				    		  UUID.fromString(rootid), (eventid), UUID.fromString(partentid), UUID.fromString(blockedparent), "B");
			    	  sendEvent( rootid,  partentid,  eventid.toString() );		
			      }				      
			      else if (servicetype.equals("I"))	// Independent, just run
			      {
			    	  sendEvent( rootid,  partentid,  eventid.toString() );					    	  
			      }
	
			  }
			  else if (sttype.equals("Update"))
			  {
				  String rootid = jsonrtoos.getString("root_service");
				  String partentid = jsonrtoos.getString("parent_service");
				  String eventid = jsonrtoos.getString("service");
				  String status = jsonrtoos.getString("status");

				  if (status.equals("Release"))	// Release
			      {
		    		  //sendEvent( rootid,  blocked_parent,  blocked_service);
				      String stquery = "SELECT *  FROM service_tree WHERE ";
				      stquery += "root_service = ";
				      stquery += rootid;
				      stquery += " AND parent_service = ";
				      stquery += eventid;
				      //System.out.println(stquery);
				      ResultSet resultSet = session.execute(stquery);
				      List<Row> all = resultSet.all();
				      for (int i = 0; i < all.size(); i++)
				      {
					      status = all.get(i).getString("status");	
					      String neweventid = all.get(i).getUUID("service").toString();	
					      if (status.equals("I"))	// kick off independent events
					      {
					    	  sendEvent( rootid,  partentid,  neweventid);		
					      }
					      else if (status.equals("C"))	// kick off contained events
					      {
					    	  sendEvent( rootid,  partentid,  neweventid );		
					      }				      
				      }

			      }
				  else if (status.equals("Complete"))	// Complete
			      {
					  String stquery = "UPDATE service_tree SET status  = 'C' WHERE root_service = ";
				      stquery += rootid;
				      stquery += " AND parent_service = ";
				      stquery += partentid;
				      stquery += " AND service = ";
				      stquery += eventid;
				      //System.out.println(stquery);
				      ResultSet resultSet = session.execute(stquery);
				      
				      // first see if this is waiting on any (contained) to finish
				      stquery = "SELECT *  FROM blocked_list WHERE ";
				      stquery += "root_service = ";
				      stquery += rootid;
				      stquery += " AND blocked_service = ";
				      stquery += eventid;
				      //System.out.println(stquery);
				      resultSet = session.execute(stquery);
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
				      if (stillwaiting == 1) return;
				      
				      // see if anyone waiting on this
				      stquery = "SELECT *  FROM blocked_list WHERE ";
				      stquery += "root_service = ";
				      stquery += rootid;
				      stquery += " AND pre_service = ";
				      stquery += eventid;
				      //System.out.println(stquery);
				      resultSet = session.execute(stquery);
				      
				      all = resultSet.all();
				      for (int i = 0; i < all.size(); i++)
				      {
					      String blocked_service = all.get(i).getUUID("blocked_service").toString();
					      String blocked_status = all.get(i).getString("status");	
					      String blocked_parent = null;
					      
					      if(blocked_status.equals("W"))
					      {
					    	  
						      // see if still blocked
						      stquery = "SELECT *  FROM blocked_list WHERE ";
						      stquery += "root_service = ";
						      stquery += rootid;
						      stquery += " AND blocked_service = ";
						      stquery += blocked_service;
						      //System.out.println(stquery);
						      resultSet = session.execute(stquery);
						      List<Row> all2 = resultSet.all();
						      int notblocked = 0;
						      // for wait, really should only have 1 record
						      // but we will loop
						      for (int ii = 0; ii < all2.size(); ii++)
						      {
							      String stillblocked = all.get(i).getString("status");	
							      if (stillblocked.equals("B"))
							      {
							    	  notblocked = 1;					    	  
							      }
						    	  
						      }
					    	  if (notblocked == 0)// simple case, parent finished, can release
					    	  {
						    	  sendEvent( rootid,  eventid,  blocked_service);				
						    	  
								  stquery = "UPDATE blocked_list SET status  = 'C'";
								  stquery += " WHERE";
							      stquery += " root_service = ";
							      stquery += rootid;
							      stquery += " AND pre_service = ";
							      stquery += eventid;
							      stquery += " AND blocked_service = ";
							      stquery += blocked_service;
							      //System.out.println(stquery);
							      resultSet = session.execute(stquery);
					    	  }				    	  
					      }
					      else if (blocked_status.equals("B"))
					      {
						      blocked_parent = all.get(i).getUUID("blocked_parent").toString();
							  stquery = "UPDATE blocked_list SET status  = 'C'";
							  stquery += " WHERE";
						      stquery += " root_service = ";
						      stquery += rootid;
						      stquery += " AND pre_service = ";
						      stquery += eventid;
						      stquery += " AND blocked_service = ";
						      stquery += blocked_service;
						     // System.out.println(stquery);
						      resultSet = session.execute(stquery);
					    	  
						      // see if still blocked
						      stquery = "SELECT *  FROM blocked_list WHERE ";
						      stquery += "root_service = ";
						      stquery += rootid;
						      stquery += " AND blocked_service = ";
						      stquery += blocked_service;
						      //System.out.println(stquery);
						      resultSet = session.execute(stquery);
						      List<Row> all2 = resultSet.all();
						      int notblocked = 0;
						      for (int ii = 0; ii < all2.size(); ii++)
						      {
							      String stillblocked = all2.get(ii).getString("status");	
							      if (stillblocked.equals("B"))
							      {
							    	  notblocked = 1;					    	  
							      }
						    	  
						      }
					    	  if (notblocked == 0)
					    	  {
					    		  // see if anyone waiting on me
					    		  //sendEvent( rootid,  blocked_parent,  blocked_service);
							      stquery = "SELECT *  FROM blocked_list WHERE ";
							      stquery += "root_service = ";
							      stquery += rootid;
							      stquery += " AND pre_service = ";
							      stquery += blocked_service;
							      //System.out.println(stquery);
							      resultSet = session.execute(stquery);
							      List<Row> all3 = resultSet.all();
							      for (int iii = 0; iii < all3.size(); iii++)
							      {
								      String stillblockedservice = all3.get(iii).getUUID("blocked_service").toString();	
								      String stillblocked = all3.get(iii).getString("status");	
								      if (stillblocked.equals("W"))
								      {
								    	  sendEvent( rootid,  blocked_service,  stillblockedservice);				
								    	  
										  stquery = "UPDATE blocked_list SET status  = 'C'";
										  stquery += " WHERE";
									      stquery += " root_service = ";
									      stquery += rootid;
									      stquery += " AND pre_service = ";
									      stquery += blocked_service;
									      stquery += " AND blocked_service = ";
									      stquery += stillblockedservice;
									      //System.out.println(stquery);
									      resultSet = session.execute(stquery);
								      }						    	  
							      }
					    	  }
					    	  
					      }
				      }
			      }
			      
			      
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
