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
 * Servlet implementation class TestServlet
 */
@WebServlet("/EvaluateBatch")
public class EvaluateBatch extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EvaluateBatch() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			  R2sLib r2lib = new R2sLib(jb.toString());
			  System.out.println("EvaluateBatch Starting: ");

			  
			  // get parameter from the message and make it a json object
		      JSONObject jsonInput =  new JSONObject(r2lib.R2s_GetParam());
			  String fileid = r2lib.R2s_GetRootID();
			  
			  String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual

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
		      //
		      // see if all good
		      //
		      int badtransaction = 0;
		      List<Row> all = resultSet.all();
		      for (int i = 0; i < all.size(); i++)
		      {			    	  
			      //  System.out.println("Transaction Found It: ");
			      String status = all.get(i).getString("status");	
			      if (status.equals("F"))	// kick off independent events
			      {
			    	  badtransaction = 1;	
			      }
		      }    
			  if ( badtransaction == 1)
			  {					  
			      System.out.println("File Failed ");
			  }
			  else
			  {
			      // //////////////////////////////////////////////////////
				  // Bulk
			      // //////////////////////////////////////////////////////
				  if (Clearing.equals("Bulk"))
				  {
					  
					  // create the bulkclear service
					  r2lib.R2s_Subsequent(FileAPI.FILEAPPURL + "/ClearBulk.html", fileid);						
				  }
			      // //////////////////////////////////////////////////////
				  // Individual
			      // //////////////////////////////////////////////////////
				  else if (Clearing.equals("Individual"))
				  {						  
				      for (int ii = 0; ii < all.size(); ii++)
				      {
				    	 // create the Individual clear service
					      String transaction_id = all.get(ii).getUUID("transaction_id").toString();	
					      r2lib.R2s_Independent(FileAPI.FILEAPPURL + "/ClearIndividual.html", transaction_id);						
				      }    
				  }
				  
			  }
		      
			  r2lib.R2s_Release();

			  // Complete triggers the release of all "successor" services			  
			  r2lib.R2s_Complete();
			  
		      System.out.println("EvaluateBatch Ending: ");
			  
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
