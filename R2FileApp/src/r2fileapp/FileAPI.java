package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

import R2sLib.R2sLib;
//import R2sLib.*;
import software.aws.mcs.auth.SigV4AuthProvider;

/**
 * Servlet implementation class R2FileAPIDBConnect()
 */
@WebServlet("/FileAPI")
public class FileAPI extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public static String FILEAPPURL = "http://R2sclient-env.eba-hnmjtifd.us-east-2.elasticbeanstalk.com";
	public static Cluster cluster = null;
	public static void DBConnect() {
		if (FileAPI.cluster== null || FileAPI.cluster.isClosed())
		{
    	FileAPI.cluster = Cluster.builder()
				.addContactPoint("cassandra.us-east-2.amazonaws.com")
				.withPort(9142)
				.withAuthProvider(new SigV4AuthProvider("us-east-2"))
                .withSSL()
				.withCredentials("rtoos-at-061466880193", "cNG6qoaLFn8w+6GYDhehcKWektKA5NKTA5SNVj4JgMg=")
				.build();
	    System.out.println("Cluster Create ");	
		}
	    return;	
	}
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileAPI() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void init(ServletConfig config) throws ServletException {
		 //FileAPI.cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
    	DBConnect();
    	

	    
    }
    
    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// jb is the buffer for the json object
	    System.out.println("FileAPI Received: ");
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
			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  JSONObject jsonObject =  new JSONObject(jb.toString());
			  R2sLib r2lib = new R2sLib();
			  String rootid = r2lib.R2s_GetID();
			  String FileName = jsonObject.getString("FileName");
			  
		      Date date = new Date();
		      //This method returns the time in millis
		      long timeMilli = date.getTime();
			  //Timestamp timestamp = new Timestamp(System.currentTimeMillis());		  
			  // data coming in in filename
			  // put to file (in cassandra)
			  FileAPI.DBConnect();
			  Session session =  FileAPI.cluster.connect();
				  session.execute("USE testapp");
			      Statement  st2 = new SimpleStatement("INSERT INTO files (file_id, file) VALUES (?, ?);", 
							UUID.fromString(rootid), FileName);
			      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
			      session.execute(st2);
				  
				  
			  jsonObject.remove("FileName");
			  
		      String lines[] = FileName.split("\\r?\\n");
			  String Clearing = jsonObject.getString("Clearing");			// Bulk or Individual
			  String Authenticate = jsonObject.getString("Authenticate");			// Bulk or Individual
		      Statement  st22 = new SimpleStatement("INSERT INTO tests (file_id, starttime, authenticate, clear, transactions, type) VALUES (?, ?, ?, ?, ?, ?);", 
						UUID.fromString(rootid), timeMilli, Authenticate, Clearing, (lines.length), "R2s" );
		      st22.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		      session.execute(st22);
			  session.close();
		      //cluster.close();

			  // get the value
			  resp = r2lib.R2s_Root(rootid, FileAPI.FILEAPPURL + "/FileImportController.html", jsonObject.toString() );
			  
		  } 
		  catch (JSONException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  resp = e.toString();
			  throw new IOException(jb.toString());
		  }

		response.getWriter().append(resp);
	}

}
