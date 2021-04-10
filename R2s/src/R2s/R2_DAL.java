package R2s;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

//import com.datastax.driver.core.Cluster;  

public class R2_DAL {
	private Map<String, JSONObject> id_to_row;
	private Map<String, ArrayList<String>> id_to_children;

	private HashMap<String, ArrayList<String>> pre_service_list;
	private HashMap<String, ArrayList<String>> blocked_service_list;
	private Map<String, JSONObject> blocked_list;
	
	private String rootid;
	
	private Cluster cluster;
	private Session session2;

	R2_DAL(Cluster cluster2)
	{
		cluster = cluster2;
		id_to_row = new HashMap<String, JSONObject>();
		id_to_children = new HashMap<String, ArrayList<String>>();

		blocked_service_list = new HashMap<String, ArrayList<String>>();
		pre_service_list = new HashMap<String, ArrayList<String>>();
		blocked_list = new HashMap<String, JSONObject>();

		session2 = cluster.connect();
		session2.execute("USE rtoos");
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
	    ResultSet resultSet = session2.execute(stquery);
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
		ResultSet resultSet = session2.execute(stquery);
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
			System.out.println("OY1");	  
	    	return false;
	    }
	    
	    String root = jsonobj.getString("root_service");
	    String parent = jsonobj.getString("parent_service");
	    String service = jsonobj.getString("service");
	    
		jsonobj.put("status", "P");
	    id_to_row.put(jsonobj.getString("service"), jsonobj);
	    String stquery = "UPDATE service_tree SET status  = 'P' WHERE ";
	    stquery += "root_service = ";
	    stquery += root;
	    stquery += " AND parent_service = ";
	    stquery += parent;
	    stquery += " AND service = ";
	    stquery += service;
	    if (consensus)
	    	stquery += " IF status = 'R'";

			
		ResultSet resultSet3 = session2.execute(stquery);
		return resultSet3.wasApplied();
		
	}
	
	public void UpdateServiceRow(JSONObject jsonobj)
	{
		String jsonquery = "INSERT INTO service_tree JSON '" + jsonobj.toString() +"'";
		session2.execute(jsonquery);
				
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
		session2.execute(jsonquery);
		
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
	      session2.execute("TRUNCATE service_tree;");
	      session2.execute("TRUNCATE blocked_list;");
		  id_to_row.clear();
		  id_to_children.clear();
	      return  "Cleaned";	
	}
	
}
