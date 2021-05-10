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
@WebServlet("/BatchController")
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
		      System.out.println("BatchController Starting: ");

		      // ours is coming in as a string buffer
			  R2sLib r2lib = new R2sLib(jb.toString());
			  
			  // get parameter from the message and make it a json object
		      //JSONObject jsonInput =  new JSONObject(r2lib.R2_GetParam());
			  
			  //System.out.println(jb.toString());
			  String fileid = r2lib.R2s_GetRootID(); 
			  serviceparam = r2lib.R2s_GetParam();
			  
			  //
			  // create the EvaluateBatch service
			  // as subsequent
			  //
			 r2lib.R2s_Subsequent(FileAPI.FILEAPPURL + "/EvaluateBatch.html", serviceparam);
			  
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
		      for (int i = 0; i < all.size(); i++)
		      {			    	  
			      //  System.out.println("Transaction Found It: ");
			      String transactionid = all.get(i).getUUID("transaction_id").toString();
			      
			      //
			      // create the authenticate service
			      // as contained
			      //
			      r2lib.R2s_Contained(transactionid, FileAPI.FILEAPPURL + "/AuthTransaction.html", "Authenticate Transaction");
				  
		      }    

			  			  
			  response.getWriter().append(resp);
		   	
			  // Release the registered services
		      //System.out.println("BatchController prerelease: ");
			  r2lib.R2s_Release();
		      //System.out.println("BatchController postrelease: ");
			  // Complete triggers the release of all "successor" services			  
			  r2lib.R2s_Complete();
			  
		      System.out.println("BatchController Ending: ");
			  
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
