package it.osm.gtfs.utils;

import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.model.GTFSStop;
import it.osm.gtfs.model.OSMStop;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * this class contains methods to automate things related to Stops
 */
public class StopsUtils {

    /***
     *
     * @param gtfsStop The GTFS stop
     * @param osmStop The OSM stop
     * @return Returns whether the two stops are the same stop or not
     */
    public static boolean match(GTFSStop gtfsStop, OSMStop osmStop) {

        double distanceBetween = OSMDistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
        String debugData = "GTFS Stop data: [" + gtfsStop + "] -> OSM Stop data: [" + osmStop +  "], exact distance between: " + distanceBetween + " m";

        if (osmStop.getCode() != null && osmStop.getCode().equals(gtfsStop.getCode())) {

            if (distanceBetween < 15 || (osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId()) && osmStop.isRevised())){
                //if the stops are less than 15m far away (with only the ref code in common) OR are already linked with gtfsid AND the OSM stop is already revised (if it has the tag that this tool creates during the import, because if the stop was already checked by a real person we know this is probably the real position of the stop. In other cases the stops can be gtfs-is-matched but the position could have been changed)
                return true;
            } else if (distanceBetween < 1000) {
                System.err.println("Warning: Too distant osm-gtfs stop (with dist > 15 m and less than 1km) / " + debugData);

                osmStop.setNeedsPositionReview(true); //the position of the osm stop needs to be reviewed as it most probably may have changed

                return true;
            }

        } else if (distanceBetween < 15 && osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId())){
            //if the stops have different ref tag code, same gtfs_id and are less than 15m far away
            System.err.println("Warning: Two stops with different ref-code tag but equal gtfs_id matched / " + debugData);
            return true;
        }

        return false;
    }


    public static Map<String, OSMStop> getGTFSIdOSMStopMap(List<OSMStop> stops) {
        final Map<String, OSMStop> result = new TreeMap<String, OSMStop>();

        for (OSMStop stop : stops){
            if (stop.getGtfsId() != null && !stop.getGtfsId().equals("")){
                result.put(stop.getGtfsId(), stop);
            }
        }

        return result;
    }

    public static Map<String, OSMStop> getOSMIdOSMStopMap(List<OSMStop> stops) {
        final Map<String, OSMStop> result = new TreeMap<String, OSMStop>();

        for (OSMStop stop : stops){
            if (stop.getOSMId() != null){
                result.put(stop.getOSMId(), stop);
            }
        }

        return result;
    }


    public static void updateOSMNodeMetadata(OSMStop osmStop){
        Element originalNode = (Element) osmStop.originalXMLNode;

        OSMXMLUtils.addOrReplaceTagValue(originalNode, "gtfs_id", osmStop.gtfsStopMatchedWith.getGtfsId());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "ref", osmStop.gtfsStopMatchedWith.getCode());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "name", GTFSImportSettings.getInstance().getPlugin().fixBusStopName(osmStop.gtfsStopMatchedWith.getName()));
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "operator", GTFSImportSettings.getInstance().getOperator());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, GTFSImportSettings.getInstance().getRevisedKey(), "no");

        //TODO: to add the wheelchair:description tag also per wiki https://wiki.openstreetmap.org/wiki/Key:wheelchair#Public_transport_stops/platforms
        WheelchairAccess gtfsWheelchairAccess = osmStop.gtfsStopMatchedWith.getWheelchairAccessibility();
        if(gtfsWheelchairAccess != null && gtfsWheelchairAccess != WheelchairAccess.UNKNOWN) {
            OSMXMLUtils.addOrReplaceTagValue(originalNode, "wheelchair", gtfsWheelchairAccess.getOsmValue());
        }

        if (osmStop.isTramStop()) {
            //OSMXMLUtils.addTagIfNotExisting(originalNode, "tram", "yes");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "public_transport", "stop_position");
        } else {
            OSMXMLUtils.addTagIfNotExisting(originalNode, "bus", "yes");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "highway", "bus_stop");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "public_transport", "platform");
        }

    }
}
