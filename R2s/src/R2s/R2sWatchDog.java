package R2s;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;

public class R2sWatchDog implements Runnable
{
	private Thread t;
	private JSONObject myrow;
	private Semaphore mysemaphore;

	R2sWatchDog (JSONObject row, Semaphore semaphore)
	{
		myrow = row;
		mysemaphore = semaphore;
	}

	   public void run() 
	   {
	   
			  try {
				  //mysemaphore.acquire();
			      R2SendThread T1 = new R2SendThread( myrow, mysemaphore);
				  Thread t = new Thread (T1, "SendThread");					  
			      t.start();
			      t.join();   // $$ may want to put a timeout here
			      
			      if (T1.retcode != 200) {
					  R2s.OnError(myrow, T1.errorstring);
			    	  
			      }
			      else {
			    	  // good send, mark complete if onerror or onfinal
			    	  String servicetype = myrow.getString("servicetype");
			    	  if (servicetype.equals("F") || servicetype.equals("E"))
						  R2s.MarkComplete(myrow);
			      }
			      
			  }
			  catch (Exception e) { 
				  try {
					R2s.OnError(myrow, e.toString());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			  }   	  
	   }


	   public void start () {
	         t = new Thread (this, "WatchDog");
	         t.start ();
 }
}
