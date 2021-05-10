package r2fileapp;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

import software.aws.mcs.auth.SigV4AuthProvider;



/**
 * Servlet implementation class R2FileAPI
 */
@WebServlet("/Tests")
public class Tests extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Tests() {
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
		// jb is the buffer for the json object
	    System.out.println("Tests Received: ");
		JSONArray temparray = new JSONArray();
		  try 
		  {
			  // data coming in in filename
			  // put to file (in cassandra)
			  Cluster cluster =  Cluster.builder()
						.addContactPoint("cassandra.us-east-2.amazonaws.com")
						.withPort(9142)
						.withAuthProvider(new SigV4AuthProvider("us-east-2"))
		                .withSSL()
						.withCredentials("rtoos-at-061466880193", "cNG6qoaLFn8w+6GYDhehcKWektKA5NKTA5SNVj4JgMg=")
						.build();
			  Session session = cluster.connect();
			  session.execute("USE testapp");
			  
		      Statement  st2 = new SimpleStatement("Select JSON * FROM tests");
		      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		      ResultSet resultSet = session.execute(st2);
		      List<Row> all = resultSet.all();
		      for (int i = 0; i < all.size(); i++)
		      {			    	  
			      //  System.out.println("Transaction Found It: ");
		    	  String jsonstr = all.get(i).getString("[json]");	
		    	  temparray.put(jsonstr);
		      }
		  
			  session.close();
		      cluster.close();

			  
		  } 
		  catch (JSONException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException("Cassandra Tests Failed");
		  }

		response.getWriter().append(temparray.toString());
	}

}
