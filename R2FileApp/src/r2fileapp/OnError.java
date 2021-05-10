package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import R2sLib.R2sLib;

//import R2sLib.*;


/**
 * Servlet implementation class FileImport
 */
@WebServlet("/OnError")
public class OnError extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OnError() {
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
			  
			  
			  String callback = request.getParameterValues("callback")[0];			  
		      System.out.println("FileImport Chain ERRORED!!!! " + callback);
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
