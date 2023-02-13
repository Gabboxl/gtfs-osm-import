package it.osm.gtfs.utils;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.models.GTFSStop;
import it.osm.gtfs.models.OSMStop;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * This class contains methods to automate things related to Stops
 */
public class StopsUtils {

    /***
     *
     * @param gtfsStop A GTFS stop
     * @param osmStop An OSM stop
     * @return Returns whether the two stops are the same stop or not
     */
    public static boolean match(GTFSStop gtfsStop, OSMStop osmStop) {
        int maxDist = 100;

        double distanceBetween = OSMDistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
        String debugData = "GTFS Stop data: [" + gtfsStop + "] -> OSM Stop data: [" + osmStop +  "], exact distance between: " + distanceBetween + " m";

        if (osmStop.getCode() != null && osmStop.getCode().equals(gtfsStop.getCode())) {

            if (distanceBetween < maxDist || (osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId()) && osmStop.isRevised())){
                //if the stops are less than maxDist far away (with only the ref code in common) OR are already linked with gtfsid AND the OSM stop is already revised (if it has the tag that this tool creates during the import, because if the stop was already checked by a real person we know this is probably the real position of the stop. In other cases the stops can be gtfs-is-matched but the position could have been changed)
                return true;
            } else if (distanceBetween < 2000 && osmStop.getOperator() != null) {//if the operator is null and that stop is too distant then it could be of another bus company/operator. so we consider it as not matched (and we will need to remove it from any list later)
                System.out.println(ansi().render("@|yellow Stop match: found too distant osm and gtfs stops / " + debugData + "|@"));


                //FIXME: we should remove this check and instead decide what to do with the stop positions that are associated to the physical stops (like move them or what during the stop gui review??)
                if(osmStop.getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP) || osmStop.getStopType().equals(OSMStopType.PHYSICAL_TRAM_STOP)) {
                    osmStop.setNeedsPositionReview(true); //the position of the osm stop needs to be reviewed as it most probably may have changed
                }

                return true;
            }

        } else if (distanceBetween < 15 && osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId())){
            //if the stops have different ref tag code, same gtfs_id and are less than 15m far away
            System.out.println(ansi().render("@|yellow Warning: Stops with different ref-code tag but equal gtfs_id matched / " + debugData + "|@"));
            return true;


        } else if(((gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP) && osmStop.getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP))
            || (gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION) && osmStop.getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION)))
                && distanceBetween < 100 && StringUtils.containsIgnoreCase(osmStop.getName(), GTFSImportSettings.getInstance().getPlugin().fixBusStopName(gtfsStop))) {
            //lol that condition is so complicated

            System.out.println(ansi().render("@|yellow Warning: Metro/train stop matched only with name / " + debugData + "|@"));

            return true;
        }

        return false;
    }


    public static Map<String, OSMStop> getGTFSIdOSMStopMap(List<OSMStop> stops) {
        final Map<String, OSMStop> result = new TreeMap<>();

        for (OSMStop stop : stops){
            if (stop.getGtfsId() != null && !stop.getGtfsId().equals("")){
                result.put(stop.getGtfsId(), stop);
            }
        }

        return result;
    }

    public static Map<String, OSMStop> getOSMIdOSMStopMap(List<OSMStop> stops) {
        final Map<String, OSMStop> result = new TreeMap<>();

        for (OSMStop stop : stops){
            if (stop.getOSMId() != null){
                result.put(stop.getOSMId(), stop);
            }
        }

        return result;
    }


    public static void updateOSMNodeMetadata(OSMStop osmStop){ //TODO: check if other tags of the node are in line with GTFS data
        Element originalNode = (Element) osmStop.originalXMLNode;

        OSMXMLUtils.addOrReplaceTagValue(originalNode, "gtfs_id", osmStop.gtfsStopMatchedWith.getGtfsId());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "ref", osmStop.gtfsStopMatchedWith.getCode());
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "name", GTFSImportSettings.getInstance().getPlugin().fixBusStopName(osmStop.gtfsStopMatchedWith));
        OSMXMLUtils.addOrReplaceTagValue(originalNode, "operator", GTFSImportSettings.getInstance().getOperator());
        

        if(GTFSImportSettings.getInstance().useRevisedKey()) {
            //we remove old only Turin-specific revised tags
            OSMXMLUtils.removeOldRevisedTag(originalNode);
            OSMXMLUtils.addOrReplaceTagValue(originalNode, GTFSImportSettings.REVISED_KEY, "no");
        }


        //TODO: to add the wheelchair:description tag also per wiki https://wiki.openstreetmap.org/wiki/Key:wheelchair#Public_transport_stops/platforms
        WheelchairAccess gtfsWheelchairAccess = osmStop.gtfsStopMatchedWith.getWheelchairAccessibility();
        if(gtfsWheelchairAccess != null && gtfsWheelchairAccess != WheelchairAccess.UNKNOWN) {
            OSMXMLUtils.addOrReplaceTagValue(originalNode, "wheelchair", gtfsWheelchairAccess.getOsmValue());
        }

        OSMStopType osmStopType = osmStop.getStopType();

        //todo: we should check for other OSM stop types if all their tags are good?
        if(osmStopType.equals(OSMStopType.PHYSICAL_BUS_STOP)) {
            OSMXMLUtils.addTagIfNotExisting(originalNode, "bus", "yes");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "highway", "bus_stop");
            OSMXMLUtils.addTagIfNotExisting(originalNode, "public_transport", "platform");
        }

    }
}
