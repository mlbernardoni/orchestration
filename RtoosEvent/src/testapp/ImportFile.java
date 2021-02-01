package testapp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
import com.datastax.driver.core.utils.UUIDs;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class ImportFile extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ImportFile() {
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
			  JSONObject jsonInput =  new JSONObject(serviceparam);
			  
			  // get the value
			  sttype = jsonrtoos.getString("type");
			  // should always be "Event" from the platform
			  if (sttype.equals("Event") )
			  {
				  //System.out.println(jb.toString());
				  String rootid = jsonrtoos.getString("root_service");
				  
				  String type = jsonInput.getString("type");					// "ImportFile"
				  String FileName = jsonInput.getString("FileName");			
				  String Authenticate = jsonInput.getString("Authenticate");	// Batch or Transaction
				  String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual
				
				  resp = jb.toString();
				  String strjason = resp;
				  System.out.println("Starting: ");
				  System.out.println(strjason);
				  
				  //
				  // first things first, setup connection to DB
				  //
				  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
				  Session session = cluster.connect();
				  session.execute("USE testapp");
				  
				  
			      // //////////////////////////////////////////////////////
			      //
			      // serviceparam is the parameter that is passed in
			      // in our case it is json, so we create jsonInput
			      //
			      // //////////////////////////////////////////////////////
				  if (Authenticate.equals("Batch"))
				  {
				      // //////////////////////////////////////////////////////
					  // Batch
				      // //////////////////////////////////////////////////////
					  BufferedReader csvReader = new BufferedReader(new FileReader(FileName));
					  String row;
					  while ((row = csvReader.readLine()) != null) 
					  {
						  //
						  // register as "contained
						  // and store in DB
						  //
						  String[] data = row.split(",");
						  String transactionid = rtooslib.RtoosContained("http://localhost:8080/RtoosEvent/Transaction.html", "Authenticate Transaction", "Register", jsonrtoos);
						  session.execute("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status) VALUES (?, ?, ?, ?, ?, ?);", 
									UUID.fromString(rootid), UUID.fromString(transactionid), data[0], data[1], data[2], "I");
					  }
					  csvReader.close();
					  //
					  // after all transactions are authenticated, Evaluate the Batch
					  //
					  rtooslib.RtoosSubsequent("http://localhost:8080/RtoosEvent/EvaluateBatch.html", serviceparam, "Register", jsonrtoos.getString("service"), jsonrtoos);
				  }
				  else if (Authenticate.equals("Transaction") )
				  {
				      // //////////////////////////////////////////////////////
					  // Transaction
				      // //////////////////////////////////////////////////////
					  if (Clearing.equals("Bulk"))
					  {
					      // //////////////////////////////////////////////////////
						  // Bulk Clearing
					      // //////////////////////////////////////////////////////					  
						  
						  // create the bulkclear service
						  String BulkClear = rtooslib.RtoosIndependant("http://localhost:8080/RtoosEvent/BulkClear.html", rootid, "Register", jsonrtoos);
						  
						  BufferedReader csvReader = new BufferedReader(new FileReader(FileName));
						  String row;
						  while ((row = csvReader.readLine()) != null) 
						  {
							  // create the authenticate service
							  // and store in DB
							  String[] data = row.split(",");
							  String transactionid = rtooslib.RtoosIndependant("http://localhost:8080/RtoosEvent/Transaction.html", "Authenticate Transaction", "Register", jsonrtoos);
							  session.execute("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status) VALUES (?, ?, ?, ?, ?, ?);", 
										UUID.fromString(rootid), UUID.fromString(transactionid), data[0], data[1], data[2], "I");
							  
							  // make authentication service predecessor to bulk clear
							  rtooslib.RtoosPredecessor(transactionid, BulkClear, jsonrtoos);						
						  }
						  csvReader.close();
					  }
					  else if (Clearing.equals("Individual"))
					  {
					      // //////////////////////////////////////////////////////
						  // Individual Clearing
					      // //////////////////////////////////////////////////////					  
					  
						  BufferedReader csvReader = new BufferedReader(new FileReader(FileName));
						  String row;
						  while ((row = csvReader.readLine()) != null) 
						  {
							  // create the authenticate service
							  // and store in DB
							  String[] data = row.split(",");
							  String transactionid = rtooslib.RtoosIndependant("http://localhost:8080/RtoosEvent/Transaction.html", "Authenticate Transaction", "Register", jsonrtoos);
							  session.execute("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status) VALUES (?, ?, ?, ?, ?, ?);", 
										UUID.fromString(rootid), UUID.fromString(transactionid), data[0], data[1], data[2], "I");
							  
							  // clear individual as subsequent
							  rtooslib.RtoosSubsequent("http://localhost:8080/RtoosEvent/ClearIndividual.html", transactionid, "Register", transactionid, jsonrtoos);						

						  }
						  csvReader.close();
					  }
				  }
				  
				  
				  // Release the registered services
				  rtooslib.RtoosUpdate("Release", jsonrtoos);
				  
				  response.getWriter().append(resp);
			   	
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
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

		//response.getWriter().append(resp);
	}
}
