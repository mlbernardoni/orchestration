package R2s;

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
					  // $$ add log error here
					  System.out.println("R2s watchdog: " + T1.errorstring);
			    	  
			      }
			      
			  }
			  catch (Exception e) { 
				  // $$ add log error here
				  System.out.println("R2s watchdog: " + e.toString());
			  }   	  
	   }


	   public void start () {
	         t = new Thread (this, "WatchDog");
	         t.start ();
 }
}
