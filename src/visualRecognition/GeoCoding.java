package visualRecognition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

public class GeoCoding {
	JSONObject res;
	public GeoCoding(double latitude, double longitude) throws JSONException, ResourceException, IOException
	{
		ClientResource resource = new ClientResource("http://dev.virtualearth.net/REST/v1/Locations/"+latitude+","+longitude+"?key=AtYkdTsT4VGt6p8vs1hybn3x8wAyDdYNN-S-95VXCQL_8vpAk44R7yzgIf4iIZ01");

		Response response = resource.getResponse();
		Client client = new Client(new Context(), Protocol.HTTP);
		ClientResource clientResource = new ClientResource("http://dev.virtualearth.net/REST/v1/Locations/"+latitude+","+longitude+"?key=AtYkdTsT4VGt6p8vs1hybn3x8wAyDdYNN-S-95VXCQL_8vpAk44R7yzgIf4iIZ01");
		clientResource.setNext(client);
		JSONObject obj= new JSONObject(clientResource.get().getText());
		JSONArray resourceSets = obj.getJSONArray("resourceSets");
		res = resourceSets.getJSONObject(0).getJSONArray("resources").getJSONObject(0).getJSONObject("address");

	}
	public String getCountry(){
		return res.getString("countryRegion");
	}
	public String getCity(){
		return res.getString("locality");
	}
}
