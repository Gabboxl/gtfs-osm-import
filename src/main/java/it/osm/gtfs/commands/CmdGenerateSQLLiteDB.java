package it.osm.gtfs.commands;

import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.models.ReadOSMRelationsResult;
import it.osm.gtfs.models.Relation;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.SharedCliOptions;
import it.osm.gtfs.utils.StopsUtils;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "sqlite", mixinStandardHelpOptions = true, description = "Generate a sqlite db containg osm relations")
public class CmdGenerateSQLLiteDB implements Callable<Void> {

    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @Override
    public Void call() throws ParserConfigurationException, IOException, SAXException {
        System.out.println("Parsing OSM stops...");
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), SharedCliOptions.checkStopsOfAnyOperatorTagValue);

        System.out.println("Indexing OSM stops...");
        Map<String, OSMStop> osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);

        System.out.println("Parsing OSM relations...");
        ReadOSMRelationsResult osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getOsmRelationsFilePath()), osmstopsOsmID);


        CmdGenerateSQLLiteDB generator = null;
        try {
            System.out.println("Creating SQLite DB...");
            generator = new CmdGenerateSQLLiteDB("gtt.db");
            generator.createDB();
            System.out.println("Adding stops to SQLite DB...");
            generator.insertStops(osmStops);
            System.out.println("Adding relations to SQLite DB...");
            generator.insertRelations(osmRels.getFinalValidRelations());
            System.out.println("Done.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (generator != null)
                generator.close();
        }
        return null;
    }

    private Connection connection = null;

    private CmdGenerateSQLLiteDB(String file) throws SQLException,
            ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file);
    }

    private void close() {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            // connection close failed.
            e.printStackTrace();
        }
    }

    private void createDB() throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30); // set timeout to 30 sec.

        statement.executeUpdate("drop table if exists relation_stops");
        statement.executeUpdate("drop table if exists relation");
        statement.executeUpdate("drop table if exists stop");
        statement.executeUpdate("create table stop (id long, ref TEXT, name TEXT, lat double, lon double, tilex long, tiley long)");
        statement.executeUpdate("create table relation (id long, ref TEXT, name TEXT, src TEXT, dst TEXT, type int)");
        statement.executeUpdate("create table relation_stops (relation_id long, stop_id long, pos long," +
                "FOREIGN KEY(relation_id) REFERENCES relation(id), FOREIGN KEY(stop_id) REFERENCES stop(id))");
        statement.executeUpdate("CREATE INDEX relations ON relation_stops(relation_id ASC)");
    }

    private void insertStops(List<OSMStop> stops) throws SQLException {
        PreparedStatement stm = connection
                .prepareStatement("insert into stop values(?, ?, ?, ?, ?, ?, ?)");
        for (OSMStop s : stops) {
            stm.setLong(1, Long.parseLong(s.getOSMId()));
            stm.setString(2, s.getCode());
            stm.setString(3, s.getName());
            stm.setDouble(4, s.getGeoPosition().getLatitude());
            stm.setDouble(5, s.getGeoPosition().getLongitude());
            stm.setLong(6, getTileX(s.getGeoPosition().getLatitude(), s.getGeoPosition().getLongitude(), 18, 256));
            stm.setLong(7, getTileY(s.getGeoPosition().getLatitude(), s.getGeoPosition().getLongitude(), 18, 256));
            stm.executeUpdate();
        }
    }

    private void insertRelations(List<Relation> rels) throws SQLException {
        PreparedStatement stm = connection.prepareStatement("insert into relation values(?, ?, ?, ?, ?, ?)");
        for (Relation r : rels) {
            stm.setLong(1, Long.parseLong(r.getId()));
            stm.setString(2, r.getRef());
            stm.setString(3, r.getName());
            stm.setString(4, r.getFrom());
            stm.setString(5, r.getTo());
            stm.setInt(6, r.getType().getGtfsValue());
            stm.executeUpdate();
        }
        stm = connection.prepareStatement("insert into relation_stops values(?, ?, ?)");
        for (Relation r : rels) {
            for (Long k:r.getStops().keySet()){
                stm.setLong(1, Long.parseLong(r.getId()));
                 OSMStop castedOsmStop = (OSMStop) r.getStops().get(k); //we force the cast to OSMStop type so we can access the getOSMId() method, this is not the best way as the stop CAN be a GTFSStop, so we need to review the types managed by the Relation class methods

                stm.setLong(2, Long.parseLong(castedOsmStop.getOSMId()));
                stm.setLong(3, k);
                stm.executeUpdate();
            }
        }
    }

    public long getTileX(final double lat, final double lon, final int zoom,
                         final int size) {
        return (long) Math.floor((lon + 180) / 360 * (1 << zoom));
    }

    public long getTileY(final double lat, final double lon, final int zoom, final int size) {
        return (long) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat))
                + 1 / Math.cos(Math.toRadians(lat)))
                / Math.PI)
                / 2 * (1 << zoom));
    }

}
