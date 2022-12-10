package it.osm.gtfs.enums;

public enum GTFSWheelchairAccess {
    UNKNOWN(0, "unknown"),
    LIMITED(1, "limited"),
    NO(2, "no");

    private final int gtfsValue;
    private final String osmValue;

    GTFSWheelchairAccess(int gtfsValue, String osmValue) {
        this.gtfsValue = gtfsValue;
        this.osmValue = osmValue;
    }


    public int getGtfsValue() {
        return gtfsValue;
    }

    public String getOsmValue() {
        return osmValue;
    }
}
