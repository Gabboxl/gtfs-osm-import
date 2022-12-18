package it.osm.gtfs.exceptions;

import it.osm.gtfs.model.GTFSStop;
import it.osm.gtfs.model.OSMStop;

public class MultipleMatchException extends Exception {
    public MultipleMatchException(GTFSStop gtfsStop, OSMStop osmStop, String errorMessage) {
        super(errorMessage);
        System.err.println("Multiple match found between this GTFS stop and other OSM stops:");
        System.err.println("current GTFS stop: " + gtfsStop);
        System.err.println("Current-matching OSM stop: " + osmStop);

        System.err.println("Already-matched OSM stop: " + gtfsStop.osmStopMatchedWith);
    }
}
