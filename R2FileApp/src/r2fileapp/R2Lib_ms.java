package r2fileapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import static org.asynchttpclient.Dsl.*;
import org.asynchttpclient.*;

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
			  connection.setConnectTimeout(60000000);		// TIMEOUT - server took to long to even accept the request
			  connection.setReadTimeout(60000000);		// TIMEOUT - server took to long to even accept the request
			  // For a PUT request
			  connection.setRequestMethod("POST");
			  connection.setRequestProperty("Content-Type", "application/json; utf-8");
			  connection.setRequestProperty("Connection", "keep-alive");
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
			  System.out.println(e);
			  throw new IOException("Error sending to Rtoos");
		  }
		  return resp.toString();

	}



	public String SendEventA(String serviceurl, String serviceparam, CountDownLatch countDownLatch) throws IOException
	{
		//HttpResponse  response;
	    String ourresponse = "SendEventA Fail1"; 
			
		try {
			   AsyncHttpClient asyncHttpClient = asyncHttpClient();


			    Request getRequest = Dsl.post(serviceurl)
			    		.addHeader("Content-Type", "application/json;charset=UTF-8")
			    		.setBody(serviceparam)
			    		.build();	    
			    ListenableFuture<Response> listenableFuture = asyncHttpClient
			    		  .executeRequest(getRequest);
	    		listenableFuture.addListener(() -> {
	    		    try {
		    			//System.out.println(serviceparam);
						listenableFuture.get();
						//Response response = listenableFuture.get();
					    //System.out.println(response.getResponseBody());
						asyncHttpClient.close();
		    		    countDownLatch.countDown();
					    
					} catch (InterruptedException e) {
					     System.out.println(e);
					} catch (ExecutionException e) {
					     System.out.println(e);
					} catch (IOException e) {
						// TODO Auto-generated catch block
					     System.out.println(e);
					}
	    		    //LOG.debug(response.getStatusCode());
	    		}, Executors.newCachedThreadPool());		
/*	    		
			    ListenableFuture<Response> execute = asyncHttpClient
				    	.preparePost(serviceurl)
				        .addHeader("Content-Type", "application/json;charset=UTF-8")
				        .setBody(serviceparam)
				        .execute();
				    ourresponse = "SendEventA Fail2"; 
			    Response response = execute.get(1200, TimeUnit.SECONDS);
			    ourresponse = "SendEventA Fail3"; 
			    System.out.println(response.getStatusCode());

			    if (response.getStatusCode() == 200) {
			    	ourresponse = response.getResponseBody();

			    }
		        asyncHttpClient.close();
		*/
	
		}
		catch (Exception e)  { 
			  // crash and burn
		     System.out.println(e);
			  throw new IOException("Error sending to Rtoos");
		}	
	    //System.out.println(ourresponse);
	    return ourresponse;
	
	}

}
