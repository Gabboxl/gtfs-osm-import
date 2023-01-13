package it.osm.gtfs.enums;

public enum OSMStopType { //data according to the GTFS reference guide - the OSM yes value is not present in GTFS reference
    BUS_STOP("bus_stop"),
    TRAM_STOP("tram_stop"),
    STOP_POSITION("stop_position");

    private final String osmStopType;

    OSMStopType(String osmStopType) {
        this.osmStopType = osmStopType;
    }


    public String getOsmStopType() {
        return osmStopType;
    }
}
