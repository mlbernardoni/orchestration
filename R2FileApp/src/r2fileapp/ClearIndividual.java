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

import R2sLib.*;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/ClearIndividual")
public class ClearIndividual extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClearIndividual() {
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

			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  R2sLib r2lib = new R2sLib(jb.toString());
			  
			  String fileid = r2lib.R2s_GetRootID();
			  String serviceparam = r2lib.R2s_GetParam();
			  		      
			  // first things first, store the transactions in DB
			  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  Session session = cluster.connect();
			  session.execute("USE testapp");

			  String stquery = "SELECT *  FROM transactions WHERE ";
		      stquery += "file_id = ";
		      stquery += fileid;
		      stquery += " AND transaction_id = ";
		      stquery += serviceparam;

		      ResultSet resultSet = session.execute(stquery);
		      List<Row> all = resultSet.all();
		      for (int i = 0; i < all.size(); i++)
		      {
		    	  
			      String status = all.get(i).getString("status");	
			      if (status.equals("V"))	// kick off independent events
			      {
					  String stquery2 = "UPDATE transactions SET status  = 'C'  WHERE file_id = " + fileid + " AND transaction_id = " + serviceparam;
					  session.execute(stquery2);
			      }
		      }
		      session.close();
		      cluster.close();

		    
			  // Complete triggers the release of all "successor" services			  
		      r2lib.R2s_Complete();
			  
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
