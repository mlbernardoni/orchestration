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

import software.aws.mcs.auth.SigV4AuthProvider;


//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class R2FileAPI
 */
@WebServlet("/FileAPI_ms")
public class FileAPI_ms extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileAPI_ms() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void init(ServletConfig config) throws ServletException {
	  	  //FileAPI_ms.cluster  = Cluster.builder().addContactPoint("127.0.0.1").build();
    }
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// jb is the buffer for the json object
	    System.out.println("FileAPI_ms Received: ");
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
			  R2Lib_ms r2lib = new R2Lib_ms();
			  String rootid = r2lib.RtoosGetID();
			  resp = rootid;
			  String FileName = jsonObject.getString("FileName");
		      Date date = new Date();
		      //This method returns the time in millis
		      long timeMilli = date.getTime();
			  
			  // data coming in in filename
			  // put to file (in cassandra)
			  FileAPI.DBConnect();
			  Session session =  FileAPI.cluster.connect();
				  session.execute("USE testapp");
				  
			      Statement  st2 = new SimpleStatement("INSERT INTO files (file_id, file) VALUES (?, ?);", 
							UUID.fromString(rootid), FileName);
			      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
			      session.execute(st2);
				  

			      String lines[] = FileName.split("\\r?\\n");
				  String Clearing = jsonObject.getString("Clearing");			// Bulk or Individual
				  String Authenticate = jsonObject.getString("Authenticate");			// Bulk or Individual
			      Statement  st22 = new SimpleStatement("INSERT INTO tests (file_id, starttime, authenticate, clear, transactions, type) VALUES (?, ?, ?, ?, ?, ?);", 
							UUID.fromString(rootid), timeMilli, Authenticate, Clearing, (lines.length), "Besoke" );
			      st22.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
			      session.execute(st22);

				  session.close();
			      //cluster.close();
		      
			  jsonObject.remove("FileName");
			    jsonObject.put("rootid", rootid);
			  // get the value
			  //  CountDownLatch countDownLatch = new CountDownLatch(1);
			  r2lib.SendEvent( FileAPI.FILEAPPURL + "/FileImportController_ms.html", jsonObject.toString() );
		    //  try {
			//	countDownLatch.await();
		   //   } catch (InterruptedException e) {
				// TODO Auto-generated catch block
		 	 //    System.out.println(e);
		     // } 
			  
			  //Timestamp timestamp2 = new Timestamp(System.currentTimeMillis());
			  //String endtime = timestamp2.toString();
		      Date date2 = new Date();
		      //This method returns the time in millis
		      long timeMilli2 = date2.getTime();
			  String fileid = rootid;
			  
			  //cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  FileAPI.DBConnect();
			  session =  FileAPI.cluster.connect();
			  session.execute("USE testapp");
			    String stquery = "UPDATE tests SET endtime  = ";
			    stquery += timeMilli2;
			    stquery += " WHERE file_id = ";
			    stquery += fileid;

			      Statement  st23 = new SimpleStatement(stquery);
			      st23.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
			      session.execute(st23);
			  session.close();
		      //cluster.close();
		  } 
		  catch (JSONException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  resp = e.toString();
			  throw new IOException(jb.toString());
		  }

	     System.out.println("FileAPI_ms Ending: ");
		response.getWriter().append(resp);
	}

}
