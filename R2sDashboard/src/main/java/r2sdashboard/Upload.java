package r2sdashboard;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Servlet implementation class Upload
 */
@WebServlet("/Upload")
public class Upload extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Upload() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    System.out.println("Upload Received: ");
		StringBuffer jb = new StringBuffer();
		String line = null;
		StringBuffer resp = new StringBuffer();
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
	
		  // if it comes in as HTML
		  // ours is coming in as a string buffer
		  JSONObject jsonin =  new JSONObject(jb.toString());
		// TODO Auto-generated method stub
		request.getParameter("file");
		String file = jsonin.getString("file");
		String auth = jsonin.getString("authdropdown");  
		String clear = jsonin.getString("cleardropdown");
		String type = jsonin.getString("type");
	    
		JSONObject jsonObject =  new JSONObject();
	    jsonObject.put("FileName", file);
	    jsonObject.put("Authenticate", auth);
	    jsonObject.put("Clearing", clear);
		
		  URL url;
		  if (type.equals("r2s"))
			  url = new URL("http://localhost:8080/R2FileApp/FileAPI.html");
		  else
			  url = new URL("http://localhost:8080/R2FileApp/FileAPI_ms.html");
		  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		  // For a PUT request
		  connection.setRequestMethod("POST");
		  connection.setRequestProperty("Content-Type", "application/json; utf-8");
		  connection.setDoOutput(true);
		  DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		  wr.writeBytes(jsonObject.toString());
		  wr.flush();
		  wr.close();
		  
		  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		  String output;
		 
		  while ((output = in.readLine()) != null) 
		  {
			  resp.append(output);
		  }
		  in.close();
		  connection.disconnect();

		  response.getWriter().append(resp);
	}

}
