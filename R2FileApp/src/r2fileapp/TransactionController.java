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

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

import R2sLib.R2sLib;

//import R2sLib.*;


/**
 * Servlet implementation class FileImport
 */
@WebServlet("/TransactionController")
public class TransactionController extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TransactionController() {
        super();
        // TODO Auto-generated constructor stub
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
			  R2sLib r2lib = new R2sLib(jb.toString());
			  
			  // get parameter from the message and make it a json object
		      JSONObject jsonInput =  new JSONObject(r2lib.R2s_GetParam());
			  
			  String fileid = r2lib.R2s_GetRootID(); 
			  
			  //String Authenticate = jsonInput.getString("Authenticate");	// Batch or Transaction
			  String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual
			
			  resp = jb.toString();
		      System.out.println("TransactionController Starting: ");
			  
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
			  //Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  FileAPI.DBConnect();
			  Session session =  FileAPI.cluster.connect();
			  session.execute("USE testapp");
			  //
			  // get all the transactions for the file
			  //
			  String stquery = "SELECT *  FROM transactions WHERE ";
		      stquery += "file_id = ";
		      stquery += fileid;
		      Statement  st2 = new SimpleStatement(stquery);
		      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		      ResultSet resultSet = session.execute(st2);
			  session.close();
		      //cluster.close();

		      List<Row> all = resultSet.all();
			  
			  
			  if (Clearing.equals("Bulk"))
			  {
				  // //////////////////////////////////////////////////////
				  // Bulk
			      // //////////////////////////////////////////////////////
				  
				  //
				  // create the bulkclear service
				  r2lib.R2s_Subsequent(FileAPI.FILEAPPURL + "/ClearBulk.html", "Clear Bulk");
			      //System.out.println(BulkClear);
				  					  
			      for (int i = 0; i < all.size(); i++)
			      {			    	  
				      //  System.out.println("Transaction Found It: ");
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      //
				      // create the authenticate service
				      //
				      r2lib.R2s_Contained(serviceid, FileAPI.FILEAPPURL + "/AuthTransaction.html", "Authenticate Transaction", 3, 6000, 6000);
			      }    
			  }
			  else if (Clearing.equals("Individual") )
			  {
			      // //////////////////////////////////////////////////////
				  // Individual
			      // //////////////////////////////////////////////////////
				  int batchsize = 0;
			      for (int i = 0; i < all.size(); i++)
			      {	
			    	  if (batchsize > 5)
			    	  {
						  r2lib.R2s_Release();
			    		  batchsize = 0;
			    	  }
				      //  System.out.println("Transaction Found It: ");
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      
				      //
				      // create the authenticate service
				      String transactionid = r2lib.R2s_Subsequent(serviceid, FileAPI.FILEAPPURL + "/AuthTransaction.html", "Authenticate Transaction");					  
				      // create the clear individual service
				      String clearid = r2lib.R2s_Independent(FileAPI.FILEAPPURL + "/ClearIndividual.html", serviceid);
				      // set transaction as predecessor to clear
				      r2lib.R2s_Setpredecessor(transactionid, clearid);
			      }    
			  }
			  
			  response.getWriter().append(resp);
		   	
			  // Release the registered services
		      //System.out.println("OY2");
			  r2lib.R2s_Release();
		      //System.out.println("OY3");
			  // Complete triggers the release of all "successor" services			  
			  r2lib.R2s_Complete();
			  
		      System.out.println("TransactionController Ending: ");
			  
		  } 
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

	}

}
