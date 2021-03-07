package r2fileapp;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

//import r2fileapp.RtoosLib;

/**
 * Servlet implementation class R2FileAPI
 */
@WebServlet("/R2FileAPI")
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
			  R2_Lib r2lib = new R2_Lib();
			  // get the value
			  resp = r2lib.R2_Root("http://localhost:8080/R2FileApp/FileImportController.html", jsonObject.toString() );
			  
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
