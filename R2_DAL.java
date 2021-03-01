package r2fileapp;
import java.util.*;

import org.json.JSONObject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

//import com.datastax.driver.core.Cluster;  

public class R2_DAL {
	private Map<String, JSONObject> id_to_row;
	private Map<String, ArrayList<JSONObject>> id_to_children;
	private String rootid;
	private Cluster cluster;
	private Session session2;

	R2_DAL(Cluster cluster2)
	{
		cluster = cluster2;
		id_to_row = new HashMap<String, JSONObject>();
		id_to_children = new HashMap<String, ArrayList<JSONObject>>();
		session2 = cluster.connect();
		session2.execute("USE rtoos");
	}
	
	public void RetrieveServiceTree(String root)
	{
		rootid = root;
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
		    	id_to_children.put(parent, new ArrayList<JSONObject>());
    		}
		    id_to_children.get(parent).add(jsonobj);
	    }	
		
		id_to_row.forEach((id, row) ->
		{
		
		});
	}
	
	public String DoClean()
	{
	      session2.execute("TRUNCATE service_tree;");
	      session2.execute("TRUNCATE blocked_list;");
		  id_to_row.clear();
		  id_to_children.clear();
	      return  "Cleaned";	
	}
	
	
	public String GetRoot()
	{
		return rootid;
	}
	
	public JSONObject GetRow(String id)
	{
		return id_to_row.get(id);
	}
	
	public void UpdateRow(String id, JSONObject jsonobj)
	{
		id_to_row.put(id, jsonobj);
		String jsonquery = "INSERT INTO service_tree JSON '" + jsonobj.toString() +"'";
		session2.execute(jsonquery);
				
		return ;
	}

	
	public ArrayList<JSONObject> GetChildren(String id)
	{
		return id_to_children.get(id);
	}
	
	public Map<String, JSONObject> GetIDtoRow()
	{
		return id_to_row;
	}

}
