package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NominatimConnection {

	private OkHttpClient client = new OkHttpClient();
	private final String nominatimBaseURL;
	
	/**
	 * constructor to pass Nominatim's base URL, e.g. http://192.168.43.201:7070/
	 * 
	 * @param nominatimURL
	 */
	public NominatimConnection(String nominatimBaseURL) {
		this.nominatimBaseURL = nominatimBaseURL;
	}
	
	public NominatimConnection()
	{
		this("http://192.168.43.201:7070");
	}
	
	public Optional<AdminPlace> askNominatim(double lat, double lon) throws IOException {
		Call call = buildCall(lat, lon);
		Response response = call.execute();
		return handleResponse(response, call);
	}
	
	private Call buildCall(double lat, double lon) {
		HttpUrl url = getURL(lat, lon);

		Request request = new Request.Builder().url(url).build();
		Call call = client.newCall(request);
		return call;
	}

	private HttpUrl getURL(double lat, double lon) {

		// 192.168.43.201:7070/reverse.php?format=jsonv2&lat=47.99870062886066&lon=12.661914825439455&zoom=18&polygon_text=1
		HttpUrl.Builder urlBuilder = HttpUrl.parse(nominatimBaseURL).newBuilder();
		urlBuilder.addPathSegment("reverse.php");
		urlBuilder.addQueryParameter("format", "jsonv2");
		urlBuilder.addQueryParameter("zoom", "18");
		urlBuilder.addQueryParameter("polygon_text", "1");
		urlBuilder.addQueryParameter("addressdetails", "1");
		urlBuilder.addQueryParameter("extratags", "1");
		urlBuilder.addQueryParameter("namedetails", "0");
		urlBuilder.addQueryParameter("accept-language", "de,en");

		urlBuilder.addQueryParameter("lat", Double.toString(lat));
		urlBuilder.addQueryParameter("lon", Double.toString(lon));

		HttpUrl url = urlBuilder.build();
		return url;
	}

	private Optional<AdminPlace> handleResponse(Response response, Call call) throws IOException {
		try (ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful()) {
				return Optional.empty();
			}

			JsonParser p = new JsonParser();
			JsonObject json = p.parse(responseBody.string()).getAsJsonObject();

			// {"error":"Unable to geocode"}
			if (json.has("error")) {
				System.err.println("Unable to geocode " + call.request());
				return Optional.empty();
			}

			String geotext = json.get("geotext").getAsString();
//			System.out.println(address);

			GeometryFactory geoFactory = new GeometryFactory();
			WKTReader wktReader = new WKTReader(geoFactory);
			Geometry geometry = wktReader.read(geotext);

//			System.out.println(geometry);

			JsonArray bboxJSON = json.get("boundingbox").getAsJsonArray();
			double x1 = bboxJSON.get(2).getAsDouble();
			double y1 = bboxJSON.get(0).getAsDouble();
			double x2 = bboxJSON.get(3).getAsDouble();
			double y2 = bboxJSON.get(1).getAsDouble();
			RectangleDouble bbox = RectangleDouble.create(x1, y1, x2, y2);

			AdminPlace place = new AdminPlace(geometry, json, bbox);
			return Optional.of(place);

		} catch (ParseException e) {
			throw new IOException(e);
		}
	}
}
