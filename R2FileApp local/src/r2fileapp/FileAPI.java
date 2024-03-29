package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;


import R2sLib.*;

/**
 * Servlet implementation class R2FileAPI
 */
@WebServlet("/FileAPI")
public class FileAPI extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileAPI() {
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
			  
			  // data coming in in filename
			  // put to file (in cassandra)
				  Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
				  Session session = cluster.connect();
				  session.execute("USE testapp");
				  
				  session.execute("INSERT INTO files (file_id, file) VALUES (?, ?);", 
							UUID.fromString(rootid), FileName);
				  
			  jsonObject.remove("FileName");
			  // get the value
			  resp = r2lib.R2s_Root(rootid, "http://localhost:8080/R2FileApp/FileImportController.html", jsonObject.toString() );
			  
		      String lines[] = FileName.split("\\r?\\n");
			  String Clearing = jsonObject.getString("Clearing");			// Bulk or Individual
			  String Authenticate = jsonObject.getString("Authenticate");			// Bulk or Individual
			  Timestamp timestamp = new Timestamp(System.currentTimeMillis());		  
			  session.execute("INSERT INTO tests (file_id, starttime, authenticate, clear, transactions, type) VALUES (?, ?, ?, ?, ?, ?);", 
						UUID.fromString(rootid), timestamp, Authenticate, Clearing, (lines.length), "R2s" );
			  session.close();
		      cluster.close();

			  
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
