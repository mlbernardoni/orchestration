package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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


import R2sLib.*;
//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class FileImport
 */
@WebServlet("/FileImportController")
public class FileImportController extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileImportController() {
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
		// jb is the buffer for the json object
		StringBuffer jb = new StringBuffer();
		String line = null;
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
			  resp = jb.toString();
		      System.out.println("FileImport Starting: ");
			  
			  // coming in as a string buffer
			  R2sLib r2lib = new R2sLib(jb.toString());
			 
			  // get parameter from the message and make it a json object
		      JSONObject jsonInput =  new JSONObject(r2lib.R2s_GetParam());
			  String rootid = r2lib.R2s_GetRootID(); 
			  serviceparam = r2lib.R2s_GetParam();
			  
			  String Authenticate = jsonInput.getString("Authenticate");	// Batch or Transaction
			  //String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual
			
		      // //////////////////////////////////////////////////////
		      //
		      // Save the transactions in the file to db
		      // in a real world situation we would have to do some sort
			  // of file duplication test to make sure it is idempotent
			  // but that is beyond the scope of this demo
		      //
		      // //////////////////////////////////////////////////////
			  //r2lib.R2s_Error("http://localhost:8080/R2FileApp/OnError.html?callback=" + rootid, "R2_Error");
			  //r2lib.R2s_Final("http://localhost:8080/R2FileApp/Finally.html?callback=" + rootid, "R2_Final");
			  r2lib.R2s_Error("http://localhost:8080/R2FileApp/OnError.html" , "R2_Error");
			  r2lib.R2s_Final("http://localhost:8080/R2FileApp/Finally.html" , "R2_Final");
//			  r2lib.R2_Final("http://127.0.0.1:46666/callback?id=" + rootid, "R2_Final");
			  //
			  // first things first, setup connection to DB
			  //
			  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  Session session = cluster.connect();
			  session.execute("USE testapp");

			  String stquery = "SELECT *  FROM files WHERE ";
		      stquery += "file_id = ";
		      stquery += rootid;
		      ResultSet resultSet = session.execute(stquery);

		      List<Row> all = resultSet.all();
		      String file = all.get(0).getString("file");
		      String lines[] = file.split("\\r?\\n");
		      for (int i = 0; i < lines.length; i++)
		      {
				  String[] data = lines[i].split(",");
				  String transactionid = r2lib.R2s_GetID();
				  session.execute("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status) VALUES (?, ?, ?, ?, ?, ?);", 
							UUID.fromString(rootid), UUID.fromString(transactionid), data[0], data[1], data[2], "I");
		    	  
		      }
		      
			  session.close();
		      cluster.close();

			  
			  if (Authenticate.equals("Batch"))
			  {
				  // //////////////////////////////////////////////////////
				  // Batch
			      // //////////////////////////////////////////////////////
				  r2lib.R2s_Subsequent("http://localhost:8080/R2FileApp/BatchController.html", serviceparam);
			  }
			  else if (Authenticate.equals("Transaction") )
			  {
			      // //////////////////////////////////////////////////////
				  // Transaction
			      // //////////////////////////////////////////////////////
				  r2lib.R2s_Subsequent("http://localhost:8080/R2FileApp/TransactionController.html", serviceparam);
			  }
			  
		   	
			  // Complete triggers the release of all "successor" services			  
			  r2lib.R2s_Release();
			  r2lib.R2s_Complete();
			  
		      System.out.println("FileImport Ending: ");
			  response.getWriter().append(resp);
			  
		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(jb.toString());
		  }

	}

}
