package it.osm.gtfs.enums;

public enum OSMStopType {
    PHYSICAL_BUS_STOP("physical_bus_stop"),
    PHYSICAL_TRAM_STOP("physical_tram_stop"),
    BUS_STOP_POSITION("bus_stop_position"),
    TRAM_STOP_POSITION("tram_stop_position"),
    GENERAL_STOP_POSITION("general_stop_position");

    private final String osmStopType;

    OSMStopType(String osmStopType) {
        this.osmStopType = osmStopType;
    }


    public String getOsmStopType() {
        return osmStopType;
    }
}
