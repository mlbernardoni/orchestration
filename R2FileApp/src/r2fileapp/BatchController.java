package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

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

//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class FileImport
 */
@WebServlet("/FileImport")
public class BatchController extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public BatchController() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		R2Lib r2lib = new R2Lib();
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
			  // ours is coming in as a string buffer
			  JSONObject jsonObject =  new JSONObject(jb.toString());
		      JSONObject jsonrtoos = (JSONObject)jsonObject.get("rtoos_msg");	  
			  
		      // //////////////////////////////////////////////////////
		      //
		      // serviceparam is the parameter that is passed in
		      // in our case it is json, so we create jsonInput
		      //
		      // //////////////////////////////////////////////////////
			  serviceparam = jsonrtoos.getString("service_param");
			  //JSONObject jsonInput =  new JSONObject(serviceparam);
			  
			  // get the value
			  sttype = jsonrtoos.getString("type");
			  // should always be "Event" from the platform
			  if (sttype.equals("Event") )
			  {
				  //System.out.println(jb.toString());
				  String fileid = jsonrtoos.getString("root_service");
				  String serviceid = jsonrtoos.getString("service");
				  
				  //String Authenticate = jsonInput.getString("Authenticate");	// Batch or Transaction
				  //String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual
				
				  resp = jb.toString();
			      System.out.println("BatchController Starting: ");
				  
				  
				  //
				  // create the EvaluateBatch service
				  // as subsequent
				  //
				 r2lib.RtoosSubsequent("http://localhost:8080/R2FileApp/EvaluateBatch.html", serviceparam, "Register", serviceid, jsonrtoos);
				  
				  //
				  // first things first, setup connection to DB
				  //
				  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
				  Session session = cluster.connect();
				  session.execute("USE testapp");
				  
				  //
				  // get all the transactions for the file
				  //
				  String stquery = "SELECT *  FROM transactions WHERE ";
			      stquery += "file_id = ";
			      stquery += fileid;
			      ResultSet resultSet = session.execute(stquery);

			      List<Row> all = resultSet.all();
			      for (int i = 0; i < all.size(); i++)
			      {			    	  
				      //  System.out.println("Transaction Found It: ");
				      String transactionid = all.get(i).getUUID("transaction_id").toString();
				      
				      //
				      // create the authenticate service
				      // as contained
				      //
				      r2lib.RtoosContained(transactionid, "http://localhost:8080/R2FileApp/AuthTransaction.html", "Authenticate Transaction", "Register", jsonrtoos);
					  
			      }    
			      session.close();
			      cluster.close();

				  
				  
				  response.getWriter().append(resp);
			   	
				  // Release the registered services
			      System.out.println("BatchController prerelease: ");
				  r2lib.RtoosRelease(jsonrtoos);
			      System.out.println("BatchController postrelease: ");
				  // Complete triggers the release of all "successor" services			  
				  r2lib.RtoosUpdate("Complete", jsonrtoos);
				  
			      System.out.println("BatchController Ending: ");
			  }
			  else 
			  {
				  
				  throw new IOException(jb.toString());
			  }
			  
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

		//response.getWriter().append(resp);
	}

}
