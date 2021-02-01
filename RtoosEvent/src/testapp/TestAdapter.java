package testapp;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class TestAdapter extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestAdapter() {
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

		
		response.getWriter().append("Input at: ").append(stinput);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		RtoosLib rtooslib = new RtoosLib();
		// jb is the buffer for the json object
		StringBuffer jb = new StringBuffer();
		String line = null;
		String sttype = null;
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
		      JSONObject jsonrtoos = (JSONObject)jsonObject.get("rtoos_msg");	  
			  // get the value
			  sttype = jsonrtoos.getString("type");
			  if (sttype.equals("Clean") )
			  {
				  resp = rtooslib.RtoosClean();
				  				  
			  }
			  else if (sttype.equals("Root") )
			  {
				  				  
				  resp = rtooslib.RtoosRoot("http://localhost:8080/RtoosEvent/TestEvent1.html", "Root" );
			  }
			  
		  } 
		  catch (JSONException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

		response.getWriter().append(resp);
	}
}
