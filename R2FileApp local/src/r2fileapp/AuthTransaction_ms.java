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
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/AuthTransaction_ms")
public class AuthTransaction_ms extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AuthTransaction_ms() {
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

  		
		response.getWriter().append("Input at: ").append(stinput);
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
			  resp = jb.toString();
			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  // coming in as a string buffer
			  
			  // get parameter from the message and make it a json object
		      //JSONObject jsonInput =  new JSONObject(r2lib.R2_GetParam());
			  String fileid = resp.split("=")[0];
			  String transactionid = resp.split("=")[1];
			  
		      
			  // first things first, store the transactions in DB
			  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  Session session = cluster.connect();
			  session.execute("USE testapp");

			  String stquery = "SELECT *  FROM transactions WHERE ";
		      stquery += "file_id = ";
		      stquery += fileid;
		      stquery += " AND transaction_id = ";
		      stquery += transactionid;
		      //System.out.println(stquery);
		      ResultSet resultSet = session.execute(stquery);
		      List<Row> all = resultSet.all();
		      for (int i = 0; i < all.size(); i++)
		      {
		    	  String newstatus = "V";
			      String fromaccount = all.get(i).getString("from_account");	
			      String toaccount = all.get(i).getString("to_account");	
			      
			      try {
			    	  Integer.parseInt(fromaccount);
			    	  Integer.parseInt(toaccount);
			      } catch (NumberFormatException nfe) {
			    	  newstatus = "F";
			      }
				  String stquery2 = "UPDATE transactions SET status  = '" + newstatus + "' WHERE file_id = " + fileid + " AND transaction_id = " + transactionid;
			      session.execute(stquery2);

		      }
		      session.close();
		      cluster.close();

		    
				  
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
