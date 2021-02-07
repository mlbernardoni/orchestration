package r2fileapp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class FileImport
 */
@WebServlet("/FileImport")
public class FileImport extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileImport() {
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
			  JSONObject jsonInput =  new JSONObject(serviceparam);
			  
			  // get the value
			  sttype = jsonrtoos.getString("type");
			  // should always be "Event" from the platform
			  if (sttype.equals("Event") )
			  {
				  //System.out.println(jb.toString());
				  String rootid = jsonrtoos.getString("root_service");
				  
				  String FileName = jsonInput.getString("FileName");			
				  String Authenticate = jsonInput.getString("Authenticate");	// Batch or Transaction
				  //String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual
				
				  resp = jb.toString();
			      System.out.println("FileImport Starting: ");
				  
			      // //////////////////////////////////////////////////////
			      //
			      // Save the transactions in the file to db
			      // in a real world situation we would have to do some sort
				  // of file duplication test to make sure it is idempotent
				  // but that is beyond the scope of this demo
			      //
			      // //////////////////////////////////////////////////////
				  //
				  // first things first, setup connection to DB
				  //
				  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
				  Session session = cluster.connect();
				  session.execute("USE testapp");
				  
				  BufferedReader csvReader = new BufferedReader(new FileReader(FileName));
				  String row;
				  while ((row = csvReader.readLine()) != null) 
				  {
					  //
					  // register as "contained
					  // and store in DB
					  //
					  String[] data = row.split(",");
					  String transactionid = r2lib.RtoosGetID();
					  session.execute("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status) VALUES (?, ?, ?, ?, ?, ?);", 
								UUID.fromString(rootid), UUID.fromString(transactionid), data[0], data[1], data[2], "I");
				  }
				  csvReader.close();

				  
				  if (Authenticate.equals("Batch"))
				  {
					  // //////////////////////////////////////////////////////
					  // Batch
				      // //////////////////////////////////////////////////////
					  r2lib.RtoosSubsequent("http://localhost:8080/R2FileApp/BatchController.html", serviceparam, "New", jsonrtoos.getString("service"), jsonrtoos);
				  }
				  else if (Authenticate.equals("Transaction") )
				  {
				      // //////////////////////////////////////////////////////
					  // Transaction
				      // //////////////////////////////////////////////////////
					  r2lib.RtoosSubsequent("http://localhost:8080/R2FileApp/TransactionController.html", serviceparam, "New", jsonrtoos.getString("service"), jsonrtoos);
				  }
				  
				  
				  response.getWriter().append(resp);
			   	
				  // Complete triggers the release of all "successor" services			  
				  r2lib.RtoosUpdate("Complete", jsonrtoos);
				  
			      System.out.println("FileImport Ending: ");
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

	}

}
