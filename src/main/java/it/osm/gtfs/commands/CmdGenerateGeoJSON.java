package it.osm.gtfs.commands;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.models.Relation;
import it.osm.gtfs.models.Stop;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.SharedCliOptions;
import it.osm.gtfs.utils.StopsUtils;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.mapfish.geo.MfFeature;
import org.mapfish.geo.MfFeatureCollection;
import org.mapfish.geo.MfGeoJSONWriter;
import org.mapfish.geo.MfGeometry;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


//TODO: is this class still useful in some way?
@CommandLine.Command(name = "geojson", description = "Generate a geojson file containg osm relations")
public class CmdGenerateGeoJSON implements Callable<Void> {

    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @Override
    public Void call() throws JSONException, ParserConfigurationException, IOException, SAXException {
        System.out.println("Parsing OSM Stops...");
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), SharedCliOptions.checkStopsOfAnyOperatorTagValue);

        System.out.println("Indexing OSM Stops...");
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);

        System.out.println("Parsing OSM Relations...");
        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getOsmRelationsFilePath()),
                osmstopsOsmID);


        CmdGenerateGeoJSON generator = null;
        try {
            System.out.println("Creating GeoJSON...");
            generator = new CmdGenerateGeoJSON();
            System.out.println("Adding Stops to GeoJSON...");
            generator.insertStops(osmStops);
            //System.err.println("Adding Relations to GeoJSON");
            //generator.insertRelations(osmRels);
            System.out.println("Done.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (generator != null)
                generator.close("data.json");
        }
        return null;
    }

    private StringBuffer buffer;
    private Collection<MfFeature> stops;
    private final GeometryFactory factory;

    private CmdGenerateGeoJSON(){
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

    private void insertStops(List<OSMStop> osmstops) throws SQLException {
        stops = new LinkedList<MfFeature>();

        for (OSMStop s : osmstops) {
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
            return new MfGeometry(new GeometryFactory().createPoint(new Coordinate(stop.getGeoPosition().getLongitude(), stop.getGeoPosition().getLatitude())));
        }
        public void toJSON(JSONWriter builder) throws JSONException {
            builder.key("name").value(stop.getName());
            builder.key("ref").value(stop.getCode());
        }
    }

}
