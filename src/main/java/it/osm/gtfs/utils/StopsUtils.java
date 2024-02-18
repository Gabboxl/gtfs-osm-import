package it.osm.gtfs.utils;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.models.OSMStop;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * This class contains methods to automate things related to Stops
 */
public class StopsUtils {

    public static List<OSMStop> getNearbyStops(OSMStop mainOsmStop, double radius, List<OSMStop> osmStopsList) {
        List<OSMStop> result = new ArrayList<>();

        for (OSMStop currentLoopStop : osmStopsList) {
            if (currentLoopStop.getStopType().equals(mainOsmStop.getStopType()) && DistanceUtils.distVincenty(mainOsmStop.getGeoPosition(), currentLoopStop.getGeoPosition()) < radius) {
                result.add(currentLoopStop);
            }
        }

        //remove the main stop from the list
        result.remove(mainOsmStop);

        return result;
    }

    public static Map<String, OSMStop> getGTFSIdOSMStopMap(List<OSMStop> stops) {
        final Map<String, OSMStop> result = new TreeMap<>();

        for (OSMStop stop : stops) {
            if (stop.getGtfsId() != null && !stop.getGtfsId().equals("")) {
                result.put(stop.getGtfsId(), stop);
            }
        }

        return result;
    }

    public static Map<String, OSMStop> getOSMIdOSMStopMap(List<OSMStop> stops) {
        final Map<String, OSMStop> result = new TreeMap<>();

        for (OSMStop stop : stops) {
            if (stop.getOSMId() != null) {
                result.put(stop.getOSMId(), stop);
            }
        }

        return result;
    }


    public static void updateOSMNodeMetadata(OSMStop osmStop) { //TODO: check if other tags of the node are in line with GTFS data
        Element originalNode = (Element) osmStop.originalXMLNode;

        OSMXMLUtils.addOrReplaceTagValue(originalNode, "gtfs_id", osmStop.gtfsStopMatchedWith.getGtfsId());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "ref", osmStop.gtfsStopMatchedWith.getCode());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "name", GTFSImportSettings.getInstance().getPlugin().fixBusStopName(osmStop.gtfsStopMatchedWith));
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "operator", GTFSImportSettings.getInstance().getOperator());

        if (GTFSImportSettings.getInstance().useRevisedKey()) {
            //we remove old only Turin-specific revised tags
            OSMXMLUtils.removeOldRevisedTag(originalNode);
            OSMXMLUtils.addOrReplaceTagValue(originalNode, GTFSImportSettings.REVISED_KEY, "no");
        }

        //TODO: to add the wheelchair:description tag also per wiki https://wiki.openstreetmap.org/wiki/Key:wheelchair#Public_transport_stops/platforms
        WheelchairAccess gtfsWheelchairAccess = osmStop.gtfsStopMatchedWith.getWheelchairAccessibility();
        if (gtfsWheelchairAccess != null && gtfsWheelchairAccess != WheelchairAccess.UNKNOWN) {
            OSMXMLUtils.addOrReplaceTagValue(originalNode, "wheelchair", gtfsWheelchairAccess.getOsmValue());
        }

        OSMStopType osmStopType = osmStop.getStopType();

        //todo: we should check for other OSM stop types if all their tags are good?
        if (osmStopType.equals(OSMStopType.PHYSICAL_BUS_STOP)) {
            OSMXMLUtils.addTagIfNotExisting(originalNode, "bus", "yes");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "highway", "bus_stop");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "public_transport", "platform");
        }

    }
}
