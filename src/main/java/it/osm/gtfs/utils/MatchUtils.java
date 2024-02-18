package it.osm.gtfs.utils;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.models.GTFSStop;
import it.osm.gtfs.models.OSMStop;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class MatchUtils {
    List<GTFSStop> globalGtfsStopsList;
    List<OSMStop> globalOsmStopsList;

    public void doStopsMatching(List<GTFSStop> gtfsStopsList, List<OSMStop> osmStopsList) {

        this.globalGtfsStopsList = gtfsStopsList;
        this.globalOsmStopsList = osmStopsList;

        //TODO: consider inverting the for loops, first osmstops and then gtfsstops, so that we can integrate the second step of the cmdgeneratebusstopsimport there directly
        for (GTFSStop gtfsStop : gtfsStopsList) {

            for (OSMStop osmStop : osmStopsList) {

                //check the match() function to understand the criteria used to consider whether the GTFS and OSM stops are the same or not
                if (match(gtfsStop, osmStop)) {
                    if (osmStop.getStopType().equals(OSMStopType.TRAM_STOP_POSITION)) { //todo: maybe add also a check for OSMStopType.PHYSICAL_TRAM_STOP ?

                        //we check for multiple matches for tram stops && bus stops, and we handle them based on how distant the current loop stop and the already matched stop are
                        if (gtfsStop.railwayStopMatchedWith != null) {
                            System.out.println(ansi().render("@|red Multiple match found between current GTFS stop and two other OSM stops: |@"));
                            System.out.println(ansi().render("@|red Current GTFS stop: |@" + gtfsStop));
                            System.out.println(ansi().render("@|red Current-matching OSM stop: |@" + osmStop));
                            System.out.println(ansi().render("@|red Already-matched OSM stop: |@" + gtfsStop.osmStopMatchedWith));

                            double distanceBetweenCurrentStop = DistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
                            double distanceBetweenAlreadyMatchedStop = DistanceUtils.distVincenty(gtfsStop.getGeoPosition(), gtfsStop.railwayStopMatchedWith.getGeoPosition());

                            if (distanceBetweenCurrentStop > distanceBetweenAlreadyMatchedStop) {

                                System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + gtfsStop.osmStopMatchedWith + "|@"));

                                continue;
                            }

                            System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + osmStop + "|@"));

                            gtfsStop.railwayStopMatchedWith.gtfsStopMatchedWith = null;
                        }

                        gtfsStop.railwayStopMatchedWith = osmStop;

                    } else {
                        if (osmStop.gtfsStopMatchedWith != null || gtfsStop.osmStopMatchedWith != null) {
                            System.out.println(ansi().render("@|red Multiple match found between current GTFS stop and two other OSM stops: |@"));
                            System.out.println(ansi().render("@|red Current GTFS stop: |@" + gtfsStop));
                            System.out.println(ansi().render("@|red Current-matching OSM stop: |@" + osmStop));
                            System.out.println(ansi().render("@|red Already-matched OSM stop: |@" + gtfsStop.osmStopMatchedWith));

                            double distanceBetweenCurrentStop = DistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
                            double distanceBetweenAlreadyMatchedStop = DistanceUtils.distVincenty(gtfsStop.getGeoPosition(), gtfsStop.osmStopMatchedWith.getGeoPosition());

                            //in case of multiple matching we check what stop is the closest one to the gtfs coordinates between the current loop stop and the already-matched stop
                            if (distanceBetweenCurrentStop > distanceBetweenAlreadyMatchedStop) {

                                System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + gtfsStop.osmStopMatchedWith + "|@"));
                                //in case the already-matched stop is the closest one to the gtfs coordinates then we skip setting the stop match variables, and we go ahead with the loop
                                continue;
                            }

                            System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + osmStop + "|@"));

                            //in case the current loop stop is the closest one to the gtfs coordinates, we remove the matched gtfs stop from the  already matched osm stop
                            gtfsStop.osmStopMatchedWith.gtfsStopMatchedWith = null;
                            //gtfsStop.osmStopMatchedWith = null; nope because we just replace 5 lines later
                        }

                        gtfsStop.stopsMatchedWith.add(osmStop);
                        osmStop.stopsMatchedWith.add(gtfsStop);

                        gtfsStop.osmStopMatchedWith = osmStop;

                    }

                    osmStop.gtfsStopMatchedWith = gtfsStop;
                }
            }
        }
    }

    /***
     *
     * @param gtfsStop A GTFS stop
     * @param osmStop An OSM stop
     * @return Returns whether the two stops are the same stop or not
     */
    public boolean match(GTFSStop gtfsStop, OSMStop osmStop) {
        int maxDist = 100;

        double distanceBetween = DistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
        String debugData = "GTFS Stop data: [" + gtfsStop + "] -> OSM Stop data: [" + osmStop + "], exact distance between: " + distanceBetween + " m";

        if (osmStop.getCode() != null && osmStop.getCode().equals(gtfsStop.getCode())) {

            if (distanceBetween < maxDist || (osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId()) && osmStop.isRevised())) {
                //if the stops are less than maxDist far away (with only the ref code in common)
                // OR (are already linked with gtfsid
                // AND the OSM stop is already marked as revised)
                // [if it has the tag that this tool creates during the import, because if the stop was already checked by a real person we know this is probably the real position of the stop.
                // In other cases the stops can be gtfs-id-matched but the position could have been changed]
                return true;
            } else if (distanceBetween < 2000 && osmStop.getOperator() != null) {//if the operator is null and that stop is too distant then it could be of another bus company/operator. so we consider it as not matched (and we will need to remove it from any list later)
                System.out.println(ansi().render("@|yellow Stop match: found too distant osm and gtfs stops / |@" + debugData));

                //FIXME: we should remove this check and instead decide what to do with the stop positions that are associated to the physical stops (like move them or what during the stop gui review??)
                if (osmStop.getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP) || osmStop.getStopType().equals(OSMStopType.PHYSICAL_TRAM_STOP)) {
                    osmStop.setNeedsPositionReview(true); //the position of the osm stop needs to be reviewed as it most probably may have changed
                }

                return true;
            }

        } else if (distanceBetween < 30 && osmStop.getGtfsId() != null && gtfsStop.getGtfsId() != null && osmStop.getGtfsId().equals(gtfsStop.getGtfsId())) {
            //if the stops have different ref tag code, same gtfs_id and are less than 15m far away
            System.out.println(ansi().render("@|yellow Warning: Stops with different ref-code tag but equal gtfs_id matched / |@" + debugData));

            return true;

        } else if (((gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP) && osmStop.getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP))
                || (gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION) && osmStop.getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION)))
                && distanceBetween < 200 && StringUtils.containsIgnoreCase(VariousUtils.removeAccents(osmStop.getName()), VariousUtils.removeAccents(GTFSImportSettings.getInstance().getPlugin().fixBusStopName(gtfsStop)))) {
            //for subway and train stations we consider the stops matched if they are less than 200m far away and have the same name

            System.out.println(ansi().render("@|yellow Warning: Metro/train stop matched only with name / |@" + debugData));

            return true;

        } else if (osmStop.getGtfsId() == null && osmStop.getCode() == null
                && (osmStop.getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP) || osmStop.getStopType().equals(OSMStopType.PHYSICAL_TRAM_STOP))
                && (gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP) || gtfsStop.getStopType().equals(OSMStopType.PHYSICAL_TRAM_STOP))
                && distanceBetween < 50 && StringUtils.equalsIgnoreCase(VariousUtils.removeAccents(osmStop.getName()), VariousUtils.removeAccents(GTFSImportSettings.getInstance().getPlugin().fixBusStopName(gtfsStop)))) {
            //remove accents from the osm stop name and try matching it with the gtfs stop name (some GTFS stops have accents, some don't)

            //check if in the range of 50m there is another stop with the same name
            var nearbyStops = StopsUtils.getNearbyStops(osmStop, 50, this.globalOsmStopsList);

            if (isAmbiguousNearbyStopPresent(nearbyStops, osmStop)) {
                System.out.println(ansi().render("@|yellow Warning: Stops with same name not matched as nearby stops have that name also / |@" + debugData));

                return false;
            }else if (nearbyStops.size() >= 1) {//this means that there are other stops with data that *could* correspond to gtfs data

                for (OSMStop nearbyStop : nearbyStops) {
                    if ((nearbyStop.getGtfsId() != null && nearbyStop.getGtfsId().equals(gtfsStop.getGtfsId()))
                            || (nearbyStop.getCode() != null && nearbyStop.getCode().equals(gtfsStop.getCode()))) {
                        //if a nearby stop has the same gtfs_id or code of GTFS data then we don't match the current main-loop osmstop

                        //TODO: consider uncommenting this output only when inverting the loop gtfs/osm up there
                        //System.out.println(ansi().render("@|yellow Warning: Stops with same name not matched as nearby stops have that name also / |@" + debugData));

                        return false;
                    }
                }

                System.out.println(ansi().render("@|yellow Warning: Stops with same name matched / |@" + debugData));

                return true;
            }

        }

        return false;
    }

    public boolean isAmbiguousNearbyStopPresent(List<OSMStop> nearbyStops, OSMStop mainOsmStop) {

        for (OSMStop nearbyStop : nearbyStops) {
            if (nearbyStop.getGtfsId() == null && nearbyStop.getCode() == null
                    && (nearbyStop.getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP) || nearbyStop.getStopType().equals(OSMStopType.PHYSICAL_TRAM_STOP))
                    && StringUtils.equalsIgnoreCase(VariousUtils.removeAccents(nearbyStop.getName()), mainOsmStop.getName())) {
                return true;
            }
        }

        return false;
    }

}


