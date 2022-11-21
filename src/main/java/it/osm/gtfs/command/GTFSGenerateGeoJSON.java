package it.osm.gtfs.command;

import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.utils.GTFSImportSetting;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfFeatureCollection;
import org.mapfish.geo.MfGeoJSONWriter;
import org.mapfish.geo.MfGeometry;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GTFSGenerateGeoJSON {

	public static void run() throws ParserConfigurationException, SAXException,
	IOException, JSONException {
		System.err.println("Parsing OSM Stops");
		List<Stop> osmStops = OSMParser.readOSMStops(GTFSImportSetting
				.getInstance().getOSMPath()
				+ GTFSImportSetting.OSM_STOP_FILE_NAME);

		System.err.println("Indexing OSM Stops");
		Map<String, Stop> osmstopsOsmID = OSMParser.applyOSMIndex(osmStops);
		
		System.err.println("Parsing OSM Relation");
		List<Relation> osmRels = OSMParser.readOSMRelations(new File(
				GTFSImportSetting.getInstance().getOSMPath()
				+ GTFSImportSetting.OSM_RELATIONS_FILE_NAME),
				osmstopsOsmID);


		GTFSGenerateGeoJSON generator = null;
		try {
			System.err.println("Creating GeoJSON");
			generator = new GTFSGenerateGeoJSON();
			System.err.println("Adding Stops to GeoJSON");
			generator.insertStops(osmStops);
			//System.err.println("Adding Relations to GeoJSON");
			//generator.insertRelations(osmRels);
			System.err.println("Done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			if (generator != null)
				generator.close("data.json");
		}
	}

	private StringBuffer buffer;
	private Collection<MfFeature> stops;
	private final GeometryFactory factory;
	
	private GTFSGenerateGeoJSON(){
		buffer = new StringBuffer();
		factory = new GeometryFactory();
	}
	
	private void close(String string) throws JSONException {
		MfFeatureCollection fc = new MfFeatureCollection(stops);
		JSONStringer stringer = new JSONStringer();
		MfGeoJSONWriter builder = new MfGeoJSONWriter(stringer);
		builder.encodeFeatureCollection(fc);
        String geojsonResulted = stringer.toString();
        System.out.println(geojsonResulted);
	}

	private void insertStops(List<Stop> osmstops) throws SQLException {
		stops = new LinkedList<MfFeature>();
		
		for (Stop s : osmstops) {
			stops.add(new JSONStop(s));
		}
	}
/*
	private void insertRelations(List<Relation> rels) throws SQLException {
	}*/
	
	public static class JSONStop extends MfFeature{
		private Stop stop;
		
		 private JSONStop(Stop stop) {
			super();
			this.stop = stop;
		}
		public String getFeatureId() {
             return "bus_stop";
         }
         public MfGeometry getMfGeometry() {
             return new MfGeometry(new GeometryFactory().createPoint(new Coordinate(stop.getLon(), stop.getLat())));
         }
         public void toJSON(JSONWriter builder) throws JSONException {
             builder.key("name").value(stop.getName());
             builder.key("ref").value(stop.getCode());
         }
	}

}
