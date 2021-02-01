package testapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
public class TestEvent1 extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestEvent1() {
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

    	//System.out.println(stinput);
		
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
			  // if it comes in as HTML
			  // ours is coming in as a string buffer
			  JSONObject jsonObject =  new JSONObject(jb.toString());
		      JSONObject jsonrtoos = (JSONObject)jsonObject.get("rtoos_msg");	  
			  // get the value
			  sttype = jsonrtoos.getString("type");
			  
			  response.getWriter().append(resp);
			  if (sttype.equals("Event") )
			  {
				  //System.out.println(jb.toString());

				  //String serviceurl = jsonrtoos.getString("service_url");
				  serviceparam = jsonrtoos.getString("service_param");

				  resp = jb.toString();
			      String strjason = resp;
			      System.out.println("Starting: ");
			      System.out.println(strjason);

				  // lets test sending a new event
				  if (serviceparam.equals("Root"))
				  {
					  // do some "news" (start right away)
					  rtooslib.RtoosIndependant("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level1 I", "New", jsonrtoos);
					  rtooslib.RtoosContained("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level1 C", "New", jsonrtoos);
					  rtooslib.RtoosSubsequent("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level1 S", "New", jsonrtoos.getString("service"), jsonrtoos);
					  
					  // register some independent services (start after release)
					  for (int i = 0; i < 2; i++)
					  {
						  rtooslib.RtoosIndependant("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level1 I", "Register", jsonrtoos);
					  }
					  // register some subsequent services (start after release)
					  for (int i = 0; i < 2; i++)
					  {
						  String newevent = rtooslib.RtoosSubsequent("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level1 S", "Register", jsonrtoos.getString("service"), jsonrtoos);
						  rtooslib.RtoosSubsequent("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level2 S", "Register", newevent, jsonrtoos);
					  }
					  // add some contained services (start after release)
					  for (int i = 0; i < 2; i++)
					  {
						  rtooslib.RtoosContained("http://localhost:8080/RtoosEvent/TestEvent1.html", "Level1 C", "Register", jsonrtoos);
					  }
					  // Release the registered services
					  rtooslib.RtoosUpdate("Release", jsonrtoos);
					  
				  }
				  else
				  {
					  TimeUnit.SECONDS.sleep(2);	// add a little wait, to see if root will end
				  }
			    
				  // Complete triggers the release of all "successor" services			  
				  rtooslib.RtoosUpdate("Complete", jsonrtoos);
				  
			      System.out.println("Ending: ");
			      System.out.println(strjason);
			  }
			  else 
			  {
				  
				  throw new IOException(jb.toString());
			  }
			  
		  } 
		  catch (JSONException | InterruptedException e) 
		  {
			  /*report an error*/ 
			  // crash and burn
			  throw new IOException(jb.toString());
		  }

		//response.getWriter().append(resp);
	}
}
