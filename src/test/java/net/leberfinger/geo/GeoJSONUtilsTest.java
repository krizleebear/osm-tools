package net.leberfinger.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

class GeoJSONUtilsTest {

	@Test
	void getCoordinateFromMultiPolygon()
	{
		String json = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[11.9015747,48.1944249],[11.9018061,48.1940296],[11.903141,48.1941765],[11.9028803,48.1946951],[11.9015747,48.1944249]]]]},\"properties\":{\"@type\":\"way\",\"@id\":3401789,\"sport\":\"horse_jumping\",\"leisure\":\"pitch\",\"surface\":\"grass\"}}";
		
		JsonParser parser = new JsonParser();
		JsonArray coordinate = GeoJSONUtils.getCoordinate(parser.parse(json).getAsJsonObject());
		assertThat(coordinate).hasSize(2);
	}

	@Test
	void getCoordinate()
	{
		String json = "{\"id\":359829,\"type\":\"node\",\"lat\":50.9049155,\"lon\":6.963953500000001,\"tags\":{\"amenity\":\"car_rental\",\"name\":\"Starcar Autovermietung\"}}";
		
		JsonParser parser = new JsonParser();
		JsonArray coordinate = GeoJSONUtils.getCoordinate(parser.parse(json).getAsJsonObject());
		assertThat(coordinate).hasSize(2);
	}

}
