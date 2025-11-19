package net.leberfinger.osm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LandusePolygonResolverTest {

	@Test
	void test() throws IOException {
		Path placesJSON = Paths.get("testplaces.geojsonseq");
		Path polyJSON = Paths.get("testlandusepolygons.geojsonseq");
		
		String pallingCenterNode = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[12.6389258,48.000387]},\"properties\":{\"@type\":\"node\",\"@id\":240041384,\"name\":\"Palling\",\"place\":\"village\",\"population\":\"3456\",\"population:date\":\"2017-06-30\"}}";
		String pallingPolygon = "{\"type\":\"FeatureCollection\", \"features\": [" + 
				"{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[12.5950766,47.9703637],[12.5953607,47.9727103],[12.6019791,47.9734364],[12.6054816,47.9756447],[12.6051771,47.9866313],[12.6019101,47.9920364],[12.6015149,48.0000915],[12.6038957,48.0018881],[12.6037128,48.0075969],[12.6080557,48.008038],[12.6067195,48.0110004],[12.6028389,48.0123523],[12.6106511,48.0155409],[12.6186272,48.0264337],[12.6133034,48.0266815],[12.6128961,48.0335542],[12.6155784,48.0330894],[12.6163547,48.0450822],[12.6224712,48.0447456],[12.6224326,48.0468012],[12.6300164,48.0474926],[12.6322431,48.049497],[12.6424822,48.0472457],[12.6451302,48.0475784],[12.6444189,48.0490697],[12.6518457,48.0493225],[12.6564979,48.0476141],[12.6741239,48.0467782],[12.6740106,48.0452744],[12.6816377,48.0458961],[12.6860289,48.0349076],[12.6826671,48.0341289],[12.6856407,48.0294477],[12.6825498,48.0252175],[12.6879822,48.0208599],[12.6911325,48.0219263],[12.6997735,48.0196999],[12.7034477,48.0119514],[12.6993748,48.0120405],[12.6979668,48.0101224],[12.7045551,48.0053099],[12.7061879,48.0062688],[12.7034737,47.9975493],[12.6958825,47.9946762],[12.6934753,47.9865004],[12.6894097,47.9856878],[12.6838769,47.9870427],[12.6761276,47.9840457],[12.6756879,47.979062],[12.6715428,47.9792927],[12.6697612,47.9762248],[12.6723981,47.9721122],[12.6678208,47.9695303],[12.6621719,47.9716884],[12.6592149,47.9703283],[12.6603152,47.9655514],[12.6566373,47.9625804],[12.6586512,47.9613793],[12.6574203,47.9598177],[12.6499559,47.959509],[12.6499691,47.9567339],[12.6433613,47.9602092],[12.6377472,47.9595468],[12.6376127,47.9616367],[12.6321195,47.9624504],[12.6286777,47.9658774],[12.6316785,47.9680828],[12.628837,47.9730173],[12.5969356,47.968864],[12.5950766,47.9703637]]]}," + 
				" \"properties\":{\"@type\":\"relation\",\"@id\":941652,\"name\":\"Palling\",\"boundary\":\"administrative\",\"wikidata\":\"Q262325\",\"wikipedia\":\"de:Palling\",\"admin_level\":\"8\",\"de:regionalschluessel\":\"091890134134\",\"TMC:cid_58:tabcd_1:Class\":\"Area\",\"TMC:cid_58:tabcd_1:LCLversion\":\"8.00\",\"TMC:cid_58:tabcd_1:LocationCode\":\"4457\",\"de:amtlicher_gemeindeschluessel\":\"09189134\"}}" + 
				"]}";
		String genghamCenterNode = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[12.6611721,47.9993422]},\"properties\":{\"@type\":\"node\",\"@id\":359566411,\"name\":\"Gengham\",\"place\":\"hamlet\"}}";
		String genghamLandusePolygon = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[12.6593538,47.999826],[12.6600588,47.9997724],[12.6601265,47.9996977],[12.6601982,47.9996224],[12.6603122,47.9995703],[12.6603135,47.999534],[12.6601553,47.9995376],[12.6601459,47.9994375],[12.6603209,47.9993989],[12.6605242,47.9992778],[12.6603445,47.9986317],[12.6598801,47.998617],[12.6599216,47.9971554],[12.6605116,47.9971913],[12.66108,47.9973948],[12.66102,47.9977856],[12.6615957,47.9978596],[12.6623396,47.9981991],[12.6630758,47.9981821],[12.6631187,47.9980744],[12.6634513,47.9980959],[12.6631724,47.9993092],[12.6623385,47.9990963],[12.6623365,48.0007275],[12.6607429,48.000979],[12.6600839,48.0009061],[12.660047,48.0005865],[12.6594613,48.0005698],[12.6593538,47.999826]]]]},\"properties\":{\"@type\":\"way\",\"@id\":32038468,\"landuse\":\"residential\"}}";
		
		String hamburgCenterNode = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[10.0099133,53.5437641]},\"properties\":{\"@type\":\"node\",\"@id\":565666208,\"ISO3166-2\":\"DE-HH\",\"name\":\"Hamburg\",\"name:be\":\"Гамбург\",\"name:cs\":\"Hamburg\",\"name:de\":\"Hamburg\",\"name:en\":\"Hamburg\",\"name:eo\":\"Hamburgo\",\"name:es\":\"Hamburgo\",\"name:fr\":\"Hambourg\",\"name:it\":\"Amburgo\",\"name:ru\":\"Гамбург\",\"name:short\":\"HH\",\"name:sk\":\"Hamburg\",\"official_name\":\"Freie und Hansestadt Hamburg\",\"official_name:be\":\"Свабодны і ганзейскі горад Гамбург\",\"place\":\"state\",\"population\":\"1783975\",\"source:population\":\"http://www.statistik-nord.de/daten/bevoelkerung-und-gebiet/monatszahlen/ 2010-07-05\",\"state_code\":\"HH\",\"wikidata\":\"Q1055\",\"wikipedia\":\"de:Hamburg\"}}";
		
		List<String> placesLines = Lists.mutable.of(pallingCenterNode, genghamCenterNode, hamburgCenterNode);
		List<String> polygonLines = Lists.mutable.of(pallingPolygon, genghamLandusePolygon);
		
		Files.write(placesJSON, placesLines);
		Files.write(polyJSON, polygonLines);
		
		LandusePolygonResolver landuse = new LandusePolygonResolver(placesJSON, polyJSON);
		landuse.resolve();
		
		Path resultJSON = Paths.get("testlandusepolygons.resolved.geojsonseq");
		assertThat(resultJSON).exists();
		List<String> result = Files.readAllLines(resultJSON);
		assertThat(result).isNotEmpty();
		assertThat(result).hasSize(2);
	}

    @Test
    void resolveOverture(){
        String oberroidhamDivisionPlace = "{ \"type\": \"Feature\", \"properties\": { \"id\": \"4be7fea6-c8dc-4de9-ae5e-eac0283270cb\", \"name\": \"Oberroidham\", \"subtype\": \"locality\", \"class\": \"hamlet\", \"names\": { \"primary\": \"Oberroidham\", \"common\": null, \"rules\": null }, \"country\": \"DE\", \"region\": \"DE-BY\", \"hierarchies\": [ [ { \"division_id\": \"567d1698-7209-4b94-b7b8-0bc71bde0104\", \"subtype\": \"country\", \"name\": \"Deutschland\" }, { \"division_id\": \"83df49db-87df-4925-80fb-1fbb9cc9802b\", \"subtype\": \"region\", \"name\": \"Bayern\" }, { \"division_id\": \"9ff5beeb-d727-443f-9b87-744d462e99ad\", \"subtype\": \"county\", \"name\": \"Landkreis Traunstein\" }, { \"division_id\": \"794a9d41-9a23-4c5c-80dd-c44cecc727c7\", \"subtype\": \"locality\", \"name\": \"Palling\" }, { \"division_id\": \"4be7fea6-c8dc-4de9-ae5e-eac0283270cb\", \"subtype\": \"locality\", \"name\": \"Oberroidham\" } ] ], \"population\": null, \"source_record_id\": \"n390754681@1\", \"wikidata\": null }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 12.6864999, 48.0127137 ] } }";
        String oberroidhamLandusePolygon = "";
    }
}
