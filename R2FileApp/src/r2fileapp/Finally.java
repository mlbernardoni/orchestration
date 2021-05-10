package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;

import R2sLib.R2sLib;

//import R2sLib.*;

/**
 * Servlet implementation class FileImport
 */
@WebServlet("/Finally")
public class Finally extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Finally() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		StringBuffer jb = new StringBuffer();
		String line = null;

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
			  // ours is coming in as a string buffer
			  R2sLib r2lib = new R2sLib(jb.toString());
		      Date date = new Date();
		      //This method returns the time in millis
		      long timeMilli = date.getTime();
			  //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			  //String endtime = timestamp.toString();
			  String fileid = r2lib.R2s_GetRootID();
			  
			  //Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  FileAPI.DBConnect();
			  Session session =  FileAPI.cluster.connect();
			  session.execute("USE testapp");
			    String stquery = "UPDATE tests SET endtime  = ";
			    stquery += timeMilli;
			    stquery += " WHERE file_id = ";
			    stquery += fileid;

			      Statement  st2 = new SimpleStatement(stquery);
			      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
			      session.execute(st2);
			  session.close();
		      //cluster.close();
			  //String callback = request.getParameterValues("callback")[0];			  
//		      System.out.println("FileImport Chain Finished!!!! " + callback);
		      System.out.println("FileImport Chain Finished!!!! " );
//		      System.out.println(r2lib.R2_GetParam());
		      
		      r2lib.R2s_Complete();
		  }
		  catch (JSONException  e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

	}

}
