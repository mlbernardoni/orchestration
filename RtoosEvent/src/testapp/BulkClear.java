package testapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class BulkClear extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public BulkClear() {
        super();
        // TODO Auto-generated constructor stub
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

    	//System.out.println(stinput);
		
		response.getWriter().append("Input at: ").append(stinput);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		RtoosLib rtooslib = new RtoosLib();
		// jb is the buffer for the json object
		StringBuffer jb = new StringBuffer();
		String line = null;
		String sttype = null;
		String resp = null;

		String serviceparam = null;
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
	
		  try 
		  {
			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  JSONObject jsonObject =  new JSONObject(jb.toString());
		      JSONObject jsonrtoos = (JSONObject)jsonObject.get("rtoos_msg");	  
			  String fileid = jsonrtoos.getString("root_service");
			  String transactionid = jsonrtoos.getString("service");
			  
			  // get the value
			  sttype = jsonrtoos.getString("type");
			  
			  if (sttype.equals("Event") )
			  {
				  //System.out.println(jb.toString());

				  //String serviceurl = jsonrtoos.getString("service_url");
				  //serviceparam = jsonrtoos.getString("service_param");	// not used, we will use service as transactionid

				  resp = jb.toString();
			      String strjason = resp;
			      System.out.println("Starting: ");
			      System.out.println(strjason);
			      
				  // first things first, store the transactions in DB
				  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
				  Session session = cluster.connect();
				  session.execute("USE testapp");

				  String stquery = "SELECT *  FROM transactions WHERE ";
			      stquery += "file_id = ";
			      stquery += fileid;
			      stquery += " AND transaction_id = ";
			      stquery += transactionid;
			      //System.out.println(stquery);
			      ResultSet resultSet = session.execute(stquery);
			      List<Row> all = resultSet.all();
			      for (int i = 0; i < all.size(); i++)
			      {
			    	  
				      System.out.println("Transaction Found It: ");
/*				      status = all.get(i).getString("status");	
				      String neweventid = all.get(i).getUUID("service").toString();	
				      if (status.equals("I"))	// kick off independent events
				      {
				    	  sendEvent( rootid,  partentid,  neweventid);		
				      }
				      else if (status.equals("C"))	// kick off contained events
				      {
				    	  sendEvent( rootid,  partentid,  neweventid );		
				      }	
*/				      
			      }
			      

			    
				  // Complete triggers the release of all "successor" services			  
				  rtooslib.RtoosUpdate("Complete", jsonrtoos);
				  
			      System.out.println("Ending: ");
			      System.out.println(strjason);
			  }
			  else 
			  {
				  
				  throw new IOException(jb.toString());
			  }
			  
		  } 
		  catch (JSONException e ) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

		  response.getWriter().append(resp);
		//response.getWriter().append(resp);
	}
}
