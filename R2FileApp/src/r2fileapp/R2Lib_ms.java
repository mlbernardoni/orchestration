package r2fileapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.datastax.driver.core.utils.UUIDs;

//import java.util.UUID;

public class R2Lib_ms {
	

    // //////////////////////////////////////////////////////
	//
	// returns an id
	//
    // //////////////////////////////////////////////////////
	public String RtoosGetID() 
	{
		return UUIDs.random().toString();
	}





	public String SendEvent(String serviceurl, String serviceparam) throws IOException
	{
		//HttpResponse  response;
/*		
		try {

			DefaultHttpClient httpClient = new DefaultHttpClient();
		    StringEntity entity = new StringEntity(strparam);
		    HttpPost post  = new HttpPost("http://localhost:8080/R2FileApp/R2.html");
		    post.setEntity(entity);
		    post.setHeader("Accept", "application/json; utf-8");
		    post.setHeader("Content-type", "application/json; utf-8");
//		    post.setHeader("Connection", "close");
//		    post.setHeader("Connection", "keep-alive");

		    response = httpClient.execute(post);
		    httpClient.close();
		    //assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
		    //httpClient.close();
		}
		catch (Exception e)  { 
			  // crash and burn
			  throw new IOException("Error sending to Rtoos");
		}
		
	    return response.toString();
*/
		  StringBuffer resp = new StringBuffer();
		  try 
		  {
			  URL url = new URL(serviceurl);
			  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			  // For a PUT request
			  connection.setRequestMethod("POST");
			  connection.setRequestProperty("Content-Type", "application/json; utf-8");
			  //connection.setRequestProperty("Connection", "keep-alive");
			  connection.setDoOutput(true);
			  DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			  wr.writeBytes(serviceparam);
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
		  }
		  catch (Exception e) 
		  { 
			  // crash and burn
			  throw new IOException("Error sending to Rtoos");
		  }
		  return resp.toString();

	}

}
