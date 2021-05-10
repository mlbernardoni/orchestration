package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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

//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class FileImport
 */
@WebServlet("/FileImportController_ms")
public class FileImportController_ms extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileImportController_ms() {
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
		      System.out.println("FileImport_ms Starting: ");
			  
			  // coming in as a string buffer
		      R2Lib_ms r2lib = new R2Lib_ms();
			  
			  // get parameter from the message and make it a json object
		      JSONObject jsonInput =  new JSONObject(jb.toString());
			  serviceparam = jsonInput.toString();
			  
		      String rootid = jsonInput.getString("rootid");	
			  String Authenticate = jsonInput.getString("Authenticate");	// Batch or Transaction
			  //String Clearing = jsonInput.getString("Clearing");			// Bulk or Individual
			
		      // //////////////////////////////////////////////////////
		      //
		      // Save the transactions in the file to db
		      // in a real world situation we would have to do some sort
			  // of file duplication test to make sure it is idempotent
			  // but that is beyond the scope of this demo
		      //
		      // //////////////////////////////////////////////////////
			  //r2lib.R2_Final("http://localhost:8080/R2FileApp/Finally.html", "R2_Final");
			  //r2lib.R2_Final("http://127.0.0.1:46666/callback?id=" + rootid, "R2_Final");
			  //
			  // first things first, setup connection to DB
			  //
			  //Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
			  FileAPI.DBConnect();
			  Session session =  FileAPI.cluster.connect();
			  session.execute("USE testapp");
			  
			  String stquery = "SELECT *  FROM files WHERE ";
		      stquery += "file_id = ";
		      stquery += rootid;
		      Statement  st2 = new SimpleStatement(stquery);
		      st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		      ResultSet resultSet = session.execute(st2);

		      List<Row> all = resultSet.all();
		      String file = all.get(0).getString("file");
		      String lines[] = file.split("\\r?\\n");
		      for (int i = 0; i < lines.length; i++)
		      {
				  String[] data = lines[i].split(",");
				  String transactionid = r2lib.RtoosGetID();
			      Statement  st22 = new SimpleStatement("INSERT INTO transactions (file_id, transaction_id, from_account, to_account, amount, status) VALUES (?, ?, ?, ?, ?, ?);", 
							UUID.fromString(rootid), UUID.fromString(transactionid), data[0], data[1], data[2], "I");
			      st22.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
			      session.execute(st22);
		    	  
		      }
		      
			  session.close();
		      //cluster.close();

			  
			  if (Authenticate.equals("Batch"))
			  {
				  // //////////////////////////////////////////////////////
				  // Batch
			      // //////////////////////////////////////////////////////
				  r2lib.SendEvent(FileAPI.FILEAPPURL + "/BatchController_ms.html", serviceparam);
			  }
			  else if (Authenticate.equals("Transaction") )
			  {
			      // //////////////////////////////////////////////////////
				  // Transaction
			      // //////////////////////////////////////////////////////
				  r2lib.SendEvent(FileAPI.FILEAPPURL + "/TransactionController_ms.html", serviceparam);
			  }
			  
		   	
			  // Complete triggers the release of all "successor" services			  
			  
		      System.out.println("FileImport_ms Ending: ");
			  response.getWriter().append(resp);
			  
		  } 
		  catch (JSONException  e) 
		  {
			  throw new IOException(jb.toString());
		  }

	}

}
