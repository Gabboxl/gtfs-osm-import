package it.osm.gtfs.utils;

import it.osm.gtfs.model.GTFSStop;
import it.osm.gtfs.model.OSMStop;
import it.osm.gtfs.model.Stop;

/**
 * this class mainly contains methods to automate things related to Stops
 */
public class StopsUtils {

    /***
     *
     * @param gtfsStop The GTFS stop
     * @param osmStop The OSM stop
     * @return Returns whether the two stops are the same or not (so whether is they are matched or not)
     */
    public static boolean matches(GTFSStop gtfsStop, OSMStop osmStop) {
        double distanceBetween = OSMDistanceUtils.distVincenty(gtfsStop.getLat(), gtfsStop.getLon(), osmStop.getLat(), osmStop.getLon());
        String debugData = "GTFS Stop data: [" + gtfsStop + "] -> OSM Stop data: [" + osmStop +  "], exact distance between: " + distanceBetween + " m";

        if (osmStop.getCode() != null && osmStop.getCode().equals(gtfsStop.getCode())){

            if (distanceBetween < 70 || (osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId()) )){
                //if the stops are less than 70m far away or are already linked with gtfsid TODO: or the revised key is already set to yes? maybe?
                return true;
            }else if (distanceBetween < 5000){
                System.err.println("Warning: Same ref tag with dist > 70 m (and less than 10km) / " + debugData);
            }

        }else if (distanceBetween < 70 && osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId())){
            //if the stops have different ref tag code, same gtfs_id and are less than 70m far away
            System.err.println("Warning: Different ref tag matched but equal gtfs_id matched / " + debugData);
            return true;
        }

        return false;
    }
}
