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


/**
 * Servlet implementation class FileImport
 */
@WebServlet("/FileImport")
public class TransactionController_ms extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TransactionController_ms() {
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
			  // coming in as a string buffer
			  R2Lib_ms r2lib = new R2Lib_ms();
			  
			  // get parameter from the message and make it a json object
		      JSONObject jsonInput =  new JSONObject(jb.toString());
			  //serviceparam = jsonInput.toString();
			  
		      String fileid =  jsonInput.getString("rootid");	
			  String Clearing = jsonInput.getString("Clearing");	// Batch or Transaction
			  
			
			  resp = jb.toString();
		      System.out.println("TransactionController_ms Starting: ");
			  
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
			  
			  
			  if (Clearing.equals("Bulk"))
			  {
				  // //////////////////////////////////////////////////////
				  // Bulk
			      // //////////////////////////////////////////////////////
				  
				  //
				  					  
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
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      //
				      // create the authenticate service
				      //
				      String parm = fileid + '=' + serviceid;
				      r2lib.SendEvent( "http://localhost:8080/R2FileApp/AuthTransaction_ms.html", parm);
			      }    
				  // create the bulkclear service
				  r2lib.SendEvent("http://localhost:8080/R2FileApp/ClearBulk_ms.html", fileid);
			      //System.out.println(BulkClear);
			  }
			  else if (Clearing.equals("Individual") )
			  {
			      // //////////////////////////////////////////////////////
				  // Individual
			      // //////////////////////////////////////////////////////
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
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      
				      //
				      // create the authenticate service
				      String parm = fileid + '=' + serviceid;
				      r2lib.SendEvent("http://localhost:8080/R2FileApp/AuthTransaction_ms.html", parm);					  
				      // create the clear individual service
				      r2lib.SendEvent("http://localhost:8080/R2FileApp/ClearIndividual_ms.html", parm);
			      }    
			  }
			  
			  session.close();
		      cluster.close();
			  response.getWriter().append(resp);
		   	
			  
		      System.out.println("TransactionController_ms Ending: ");
			  
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

	}

}
