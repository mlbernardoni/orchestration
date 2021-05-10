package R2s;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import software.aws.mcs.auth.SigV4AuthProvider;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;


//import com.datastax.driver.core.Cluster;  

public class R2s_DAL {
	public static Cluster r2scluster;
	private static String CASSANDRA_URL = "127.0.0.1";
	private static Integer CASSANDRA_PORT = 0;
	private static String CASSANDRA_AUTH = "";
	private static String CASSANDRA_USER = ""; 
	private static String CASSANDRA_PASSWORD = ""; 
	private LinkedHashMap<String, JSONObject> id_to_row;
	private LinkedHashMap<String, ArrayList<String>> id_to_children;

	private LinkedHashMap<String, ArrayList<String>> pre_service_list;
	private LinkedHashMap<String, ArrayList<String>> blocked_service_list;
	private LinkedHashMap<String, JSONObject> blocked_list;
	
	private String rootid;
	
	private Session session2;

	
	static public void create() {
		try {
			
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();         
			//URL str = classLoader.getResource("../R2sConfiguration.cfg");
			InputStream stream = classLoader.getResourceAsStream("../R2sConfiguration.cfg");
			//InputStream stream = classLoader.getResourceAsStream("../R2sConfigurationERROR.cfg");
			
			if (stream == null) {
			    System.out.println("R2sConfiguration.json missing from WEB-INF folder");
			    // might as well try with default
				r2scluster = Cluster.builder().addContactPoint(CASSANDRA_URL).build();		
				return;
			}
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int length; (length = stream.read(buffer)) != -1; ) {
			     result.write(buffer, 0, length);
			}
			//stream.close();
			// StandardCharsets.UTF_8.name() > JDK 7
			JSONObject r2sconifg =  new JSONObject(result.toString("UTF-8"));
			CASSANDRA_URL = r2sconifg.getString("CASSANDRA_URL");
			CASSANDRA_PORT = r2sconifg.getInt("CASSANDRA_PORT");
			CASSANDRA_AUTH = r2sconifg.getString("CASSANDRA_AUTH");
			CASSANDRA_USER = r2sconifg.getString("CASSANDRA_USER");
			CASSANDRA_PASSWORD = r2sconifg.getString("CASSANDRA_PASSWORD");

			r2scluster = Cluster.builder()
					.addContactPoint(CASSANDRA_URL)
					.withPort(CASSANDRA_PORT)
					.withAuthProvider(new SigV4AuthProvider(CASSANDRA_AUTH))
                    .withSSL()
					.withCredentials(CASSANDRA_USER, CASSANDRA_PASSWORD)
					.build();
		} 
	catch (IOException e) {
	      System.out.println(e.toString());
		}

	}
	static public void destroy() {
    	r2scluster.close();	// not sure this does anything		
	}

	public void Init() throws IOException
	{
		id_to_row = new LinkedHashMap<String, JSONObject>();
		id_to_children = new LinkedHashMap<String, ArrayList<String>>();

		blocked_service_list = new LinkedHashMap<String, ArrayList<String>>();
		pre_service_list = new LinkedHashMap<String, ArrayList<String>>();
		blocked_list = new LinkedHashMap<String, JSONObject>();
		
		int tries = 50;
		while (tries > 0)
		{
			try {
				
				session2 = r2scluster.connect();
				session2.execute("USE rtoos");
				return;
			}
			catch(Exception e) {
				tries --;
				  System.out.println("R2s Waiting for Cassandra: " + e.toString());
				  if (tries > 0) {
					  try 
					  {
						  TimeUnit.MILLISECONDS.sleep(5000);	// add a little wait, to see if root will end
						  create();
					  }
					  catch (JSONException | InterruptedException ie) 
					  {
						  throw new IOException("InterruptedException " + ie.toString());
					  }						  
				  }
			}
		}

	}
    public void CleanUp() {
    	
    	session2.close();	// not sure this does anything
    }
    
	
	public void UpdateServices()
	{
		
		id_to_row.clear();
		id_to_children.clear();

		String stquery = "SELECT JSON * FROM service_tree WHERE ";
		stquery += "root_service = ";
		stquery += rootid;
		Statement  st = new SimpleStatement(stquery);
	    st.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		if (r2scluster== null || r2scluster.isClosed()) create();		
	    ResultSet resultSet = session2.execute(st);
	    List<Row> all = resultSet.all();
	    for (int i = 0; i < all.size(); i++)
	    {
	    	String jsonstr = all.get(i).getString("[json]");
	    	JSONObject jsonobj =  new JSONObject(jsonstr);
	    	
		    //String status = all.get(i).getString("status");
		    //String servicetype = all.get(i).getString("servicetype");
		    String service = jsonobj.getString("service");
		    String parent = jsonobj.getString("parent_service");
		    id_to_row.put(service, jsonobj);
		    
		    if (id_to_children.get(parent) == null)
    		{
		    	id_to_children.put(parent, new ArrayList<String>());
    		}
		    id_to_children.get(parent).add(service);
	    }	
	}
	
	public void UpdateBlocked()
	{
		pre_service_list.clear();
		blocked_service_list.clear();
		blocked_list.clear();

		String stquery = "SELECT JSON * FROM blocked_list WHERE ";
		stquery += "root_service = ";
		stquery += rootid;
		Statement  st = new SimpleStatement(stquery);
	    st.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		if (r2scluster== null || r2scluster.isClosed()) create();		
		ResultSet resultSet = session2.execute(st);
		List<Row> all = resultSet.all();
	    for (int i = 0; i < all.size(); i++)
	    {
	    	String jsonstr = all.get(i).getString("[json]");
	    	JSONObject jsonobj =  new JSONObject(jsonstr);
	    	
		    String pre = jsonobj.getString("pre_service");
		    String blocked = jsonobj.getString("blocked_service");
		    String key = pre + blocked;
		    
		    blocked_list.put(key, jsonobj);

		    if (pre_service_list.get(blocked) == null)
    		{
		    	pre_service_list.put(blocked, new ArrayList<String>());
    		}
		    pre_service_list.get(blocked).add(key);

		    
		    if (blocked_service_list.get(pre) == null)
    		{
		    	blocked_service_list.put(pre, new ArrayList<String>());
    		}
		    blocked_service_list.get(pre).add(key);
	  }	
	}

	
	public void RetrieveServiceTree(String root)
	{
		rootid = root;
		
		UpdateServices();
		
		UpdateBlocked();
	}

	private JSONArray RenderChildren(String node) {
	    //System.out.println(node);
		JSONArray temparray = new JSONArray();
		
		ArrayList<String> childarray = id_to_children.get(node);
		if (childarray == null)
		{
		    //System.out.println("OY");
			return temparray;
		}
		
		for (int i = 0; i < childarray.size(); i++)
		{
			if (!node.equals(childarray.get(i))) 	// root is a parent to itself, so bail
			{
		    	JSONObject jsonnode = new JSONObject();		
				JSONObject row = id_to_row.get(childarray.get(i));
		    	jsonnode.put("record", row);
		    	JSONArray newchildarray = RenderChildren(childarray.get(i));
		    	jsonnode.put("children", newchildarray);
		    	temparray.put(jsonnode);
			}
				
		}
		
		return temparray;
	}
	
	public String RetrieveSearchList(JSONObject jsonObject) 
	{

		long starttime =  jsonObject.getBigInteger("starttime").longValue();	  
		long endtime = jsonObject.getBigInteger("endtime").longValue();	  	
		Timestamp starttimel = new Timestamp(starttime);		  
		Timestamp endtimel = new Timestamp(endtime);		  
	    //System.out.println(starttimel);
	    //System.out.println(endtimel);

		JSONArray newchildarray = new JSONArray();
		String stquery = "select JSON root_service, create_date, service from rtoos.service_tree";
//		String stquery = "select distinct root_service from rtoos.service_tree";
		Statement  st = new SimpleStatement(stquery);
	    st.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		if (r2scluster== null || r2scluster.isClosed()) create();		
		ResultSet resultSet = session2.execute(st);
		List<Row> all = resultSet.all();
	    //System.out.println(all.size());
		String temproot = "";
	    for (int i = 0; i < all.size(); i++)
	    {
	    	String jsonstrroot = all.get(i).getString("[json]");
			JSONObject jsonroot =  new JSONObject(jsonstrroot);
		    //System.out.println("OY1");
	    	String jsonstr = jsonroot.getString("root_service").toString();
	    	String rootdate = jsonroot.getString("create_date");
	    	String jsonserv = jsonroot.getString("service");
	    	if (!temproot.equals(jsonstr) && jsonstr.equals(jsonserv)) {
	    		temproot = jsonstr;
				String stquery2 = "select JSON * from rtoos.service_tree where root_service = " + jsonstr + " and create_date = '" + rootdate + "' and service = " + jsonstr;
				Statement  st2 = new SimpleStatement(stquery2);
				st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
				if (r2scluster== null || r2scluster.isClosed()) create();		
				ResultSet resultSet2 = session2.execute(st2);
				List<Row> all2 = resultSet2.all();
				if (all2 != null) {
					
			    	String jsonstr2 = all2.get(0).getString("[json]");
				    //System.out.println("OY2");
				    //System.out.println(jsonstr2);
					JSONObject jsonrow =  new JSONObject(jsonstr2);
			    	String jsondate = jsonrow.getString("create_date");
			    	jsondate = jsondate.replace(' ', 'T');
				    //System.out.println(jsondate);
				    Instant instant = Instant.parse ( jsondate );
			    	Timestamp createl = Timestamp.from(instant);
				    //System.out.println("OY3");
				    //System.out.println(createl); 
			    	if(createl.after(starttimel) && createl.before(endtimel))
			    	{
						newchildarray.put(jsonrow);	    		
			    	}
				}
	    		
	    	}
	    }
	    //System.out.println("OY1");
		return newchildarray.toString();
	}
	
	public String RetrieveJsonTree(String root)
	{
    	JSONObject jsonnode = new JSONObject();		
		JSONObject row = id_to_row.get(root);
    	jsonnode.put("record", row);
    	JSONArray newchildarray = RenderChildren(root);
    	jsonnode.put("children", newchildarray);				
		
	    //System.out.println(jsonnode.toString());
	    return jsonnode.toString();
	}
	
	public String GetRoot()
	{
		return rootid;
	}
	
	// ///////////////////////////////////////
	//
	// service_tree functions
	//
	// //////////////////////////////////////
	public JSONObject GetServiceRow(String id)
	{
		return id_to_row.get(id);
	}
	
	// will return true if this service has not already been sent
	public boolean UpdateSendStatus(JSONObject jsonobj, boolean consensus)
	{
	    
		// not sure if this check will help or not, can't hurt
	    if (!jsonobj.getString("status").equals("R"))
	    {
			//System.out.println("OY1");	  
	    	return false;
	    }
	    
	    String root = jsonobj.getString("root_service");
	    String create_date = jsonobj.getString("create_date");
		//System.out.println(create_date);	  
	    String service = jsonobj.getString("service");
	    
		jsonobj.put("status", "P");
	    id_to_row.put(jsonobj.getString("service"), jsonobj);
	    String stquery = "UPDATE service_tree SET status  = 'P' WHERE ";
	    stquery += "root_service = ";
	    stquery += root;
	    stquery += " AND create_date = '";
	    stquery += create_date;
	    stquery += "' AND service = ";
	    stquery += service;
	    if (consensus)  // this is busted on subsequent to contained and really is a cheat anyway $$$
	    	stquery += " IF status = 'R'";

			
		//System.out.println(stquery);	  
		Statement  st2 = new SimpleStatement(stquery);
		st2.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		if (r2scluster== null || r2scluster.isClosed()) create();		
		ResultSet resultSet3 = session2.execute(st2);
		return resultSet3.wasApplied();
		
	}
	
	public void UpdateServiceRow(JSONObject jsonobj)
	{
		String jsonquery = "INSERT INTO service_tree JSON '" + jsonobj.toString() +"'";
		Statement  st = new SimpleStatement(jsonquery);
		  st.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);

		//System.out.println(st);
		if (r2scluster== null || r2scluster.isClosed()) create();		
		session2.execute(st);
				
	    String service = jsonobj.getString("service");
	    String parent = jsonobj.getString("parent_service");
	    id_to_row.put(service, jsonobj);
	    
	    if (id_to_children.get(parent) == null)
		{
	    	id_to_children.put(parent, new ArrayList<String>());
		}
	    id_to_children.get(parent).add(service);
	    
	    // we don't have to get the data again after an update to service tree
	    // as the service is the only one hitting this
		return ;
	}

	
	public ArrayList<JSONObject> GetServiceChildren(String id)
	{
		ArrayList<JSONObject> retlist = new ArrayList<JSONObject>();
		ArrayList<String> keys = id_to_children.get(id);
	      if (keys != null)
	      {
		      for (int i = 0; i < keys.size(); i++)
		      {
		    	  String key = keys.get(i);
		    	  retlist.add(id_to_row.get(key));
		      }
	      }
		return retlist;
	}
	
	
	// ///////////////////////////////////////
	//
	// not currently in use, did a little trial with R2 
	// to iterate through everything
	//
	// //////////////////////////////////////
	public Map<String, JSONObject> GetServiceIDtoRow()
	{
		return id_to_row;
	}

	// ///////////////////////////////////////
	//
	// blocked_list functions
	//
	// //////////////////////////////////////

	public JSONObject GetBlockedRow(String pre, String blocked)
	{
		String key = pre + blocked;
		return blocked_list.get(key);
	}

	public void UpdateBlockedRow(JSONObject blockedrow)
	{
		String jsonquery = "INSERT INTO blocked_list JSON '" + blockedrow.toString() +"'";
		Statement  st = new SimpleStatement(jsonquery);
	    st.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
		if (r2scluster== null || r2scluster.isClosed()) create();		
		session2.execute(st);
		
	    String pre_service = blockedrow.getString("pre_service");
	    String blocked_service = blockedrow.getString("blocked_service");
		String key = pre_service + blocked_service;
	    blocked_list.put(key, blockedrow);
	    
		return;
	}
	
	public ArrayList<JSONObject> GetBlockedServices(String id)
	{
		ArrayList<JSONObject> retlist = new ArrayList<JSONObject>();
		ArrayList<String> keys = blocked_service_list.get(id);
	      if (keys != null)
	      {
		      for (int i = 0; i < keys.size(); i++)
		      {
		    	  String key = keys.get(i);
		    	  retlist.add(blocked_list.get(key));
		      }
	      }

		return retlist;
	}
	
	public ArrayList<JSONObject> GetPreServices(String id)
	{
		ArrayList<JSONObject> retlist = new ArrayList<JSONObject>();
		ArrayList<String> keys = pre_service_list.get(id);
	    //System.out.println(id);
	    //if (keys != null)  System.out.println(keys.size());
	    //System.out.println(keys);
	      if (keys != null)
	      {
		      for (int i = 0; i < keys.size(); i++)
		      {
		    	  String key = keys.get(i);
		    	  retlist.add(blocked_list.get(key));
		      }
	      }

		return retlist;
	}
	

	
	
	public String DoClean()
	{
		if (r2scluster== null || r2scluster.isClosed()) create();		
	      session2.execute("TRUNCATE service_tree;");
	      session2.execute("TRUNCATE blocked_list;");
		  id_to_row.clear();
		  id_to_children.clear();
	      return  "Cleaned";	
	}
	
}
