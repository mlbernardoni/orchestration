package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class R2FileAPI
 */
@WebServlet("/R2FileAPI")
public class FileAPI_ms extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileAPI_ms() {
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
			  
			  // data coming in in filename
			  // put to file (Temp Directory)
			  // future, this will be to S3
			  // $$$$$$$$$$$$
		      File file = File.createTempFile(rootid, ".tmp");
		        
		         BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		         bw.write(FileName);
		 
		         bw.close();

		      
		      System.out.println(file.getAbsolutePath());
			    
			    jsonObject.remove("FileName");
			    jsonObject.put("FileName", file.getAbsolutePath());
			    jsonObject.put("rootid", rootid);
			  // get the value
			  //  CountDownLatch countDownLatch = new CountDownLatch(1);
			  r2lib.SendEvent( "http://localhost:8080/R2FileApp/FileImportController_ms.html", jsonObject.toString() );
		    //  try {
			//	countDownLatch.await();
		   //   } catch (InterruptedException e) {
				// TODO Auto-generated catch block
		 	 //    System.out.println(e);
		     // } 
			  
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
