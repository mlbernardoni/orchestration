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

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/ClearIndividual_ms")
public class ClearIndividual_ms extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClearIndividual_ms() {
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

		//String serviceparam = null;
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

			  // get parameter from the message and make it a json object
		      //JSONObject jsonInput =  new JSONObject(r2lib.R2_GetParam());
			  String fileid = resp.split("=")[0];
			  String transactionid = resp.split("=")[1];
			  		      
			  // first things first, store the transactions in DB
			  //Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  FileAPI.DBConnect();
			  Session session =  FileAPI.cluster.connect();
			  session.execute("USE testapp");

			  String stquery = "SELECT *  FROM transactions WHERE ";
		      stquery += "file_id = ";
		      stquery += fileid;
		      stquery += " AND transaction_id = ";
		      stquery += transactionid;

		      Statement  st2 = new SimpleStatement(stquery);
		      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		      ResultSet resultSet = session.execute(st2);
		      List<Row> all = resultSet.all();
		      for (int i = 0; i < all.size(); i++)
		      {
		    	  
			      String status = all.get(i).getString("status");	
			      if (status.equals("V"))	// kick off independent events
			      {
					  String stquery2 = "UPDATE transactions SET status  = 'C'  WHERE file_id = " + fileid + " AND transaction_id = " + transactionid;
				      Statement  st22 = new SimpleStatement(stquery2);
				      st22.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
					  session.execute(st22);
			      }
		      }

		      session.close();
		      //cluster.close();
		    
			  
//			      System.out.println("ClearIndividual Ending: ");
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
