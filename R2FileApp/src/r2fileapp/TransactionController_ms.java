package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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


/**
 * Servlet implementation class FileImport
 */
@WebServlet("/TransactionController_ms")
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
			  
			  
			  if (Clearing.equals("Bulk"))
			  {
				  // //////////////////////////////////////////////////////
				  // Bulk
			      // //////////////////////////////////////////////////////
				  
				  //
				  					  

			      List<Row> all = resultSet.all();
			      for (int i = 0; i < all.size(); i++)
			      {			    	  
				      //  System.out.println("Transaction Found It: ");
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      //
				      // create the authenticate service
				      //
				      String parm = fileid + '=' + serviceid;
				      r2lib.SendEvent( FileAPI.FILEAPPURL + "/AuthTransaction_ms.html", parm);
			      }    
				  // create the bulkclear service
				  r2lib.SendEvent(FileAPI.FILEAPPURL + "/ClearBulk_ms.html", fileid);
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
			      List<Row> all = resultSet.all();
			      for (int i = 0; i < all.size(); i++)
			      {			    	  
				      //  System.out.println("Transaction Found It: ");
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      
				      //
				      // create the authenticate service
				      String parm = fileid + '=' + serviceid;
				      r2lib.SendEvent(FileAPI.FILEAPPURL + "/AuthTransaction_ms.html", parm);					  
				      r2lib.SendEvent(FileAPI.FILEAPPURL + "/ClearIndividual_ms.html", parm);
			      }    
			  }
			  else if (Clearing.equals("IndividualA") )
			  {
			      // //////////////////////////////////////////////////////
				  // Individual
			      // //////////////////////////////////////////////////////
				  //
				  // get all the transactions for the file
				  //
			      List<Row> all = resultSet.all();
				  CountDownLatch countDownLatch = new CountDownLatch(all.size());
			      for (int i = 0; i < all.size(); i++)
			      {			    	  
				      //  System.out.println("Transaction Found It: ");
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      
				      //
				      // create the authenticate service
				      String parm = fileid + '=' + serviceid;
				      r2lib.SendEventA(FileAPI.FILEAPPURL + "/AuthTransaction_ms.html", parm, countDownLatch);					  
			      }    
			      try {
						countDownLatch.await();
				      } catch (InterruptedException e) {
						// TODO Auto-generated catch block
				 	     System.out.println(e);
				      } 
				  CountDownLatch countDownLatch2 = new CountDownLatch(all.size());
			      for (int i = 0; i < all.size(); i++)
			      {			    	  
				      //  System.out.println("Transaction Found It: ");
				      String serviceid = all.get(i).getUUID("transaction_id").toString();
				      
				      //
				      // create the authenticate service
				      String parm = fileid + '=' + serviceid;
				      r2lib.SendEventA(FileAPI.FILEAPPURL + "/ClearIndividual_ms.html", parm, countDownLatch2);
			      }    
			      try {
						countDownLatch2.await();
				      } catch (InterruptedException e) {
						// TODO Auto-generated catch block
				 	     System.out.println(e);
				      } 
			  }
			  
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
