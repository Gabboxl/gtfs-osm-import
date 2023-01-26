package it.osm.gtfs.enums;

public enum RouteType { //data according to the GTFS reference guide - the OSM yes value is not present in GTFS reference
    TRAM_LIGHT_RAIL(0, "tram"),
    SUBWAY_METRO(1, "subway"),
    RAIL(2, "train"),
    BUS(3, "bus"),
    FERRY(4, "ferry"),
    CABLE_TRAM(5, "tram"),
    AERIAL_LIFT(6, "funicular"),
    FUNICULAR(7, "funicular"),
    TROLLEYBUS(11, "trolleybus"),
    MONORAIL(12, "monorail");

    private final int gtfsValue;
    private final String osmValue;

    RouteType(int gtfsValue, String osmValue) {
        this.gtfsValue = gtfsValue;
        this.osmValue = osmValue;
    }

    public static RouteType getEnumByOsmValue(String osmValue){
        for(RouteType e : RouteType.values()){
            if(e.osmValue.equals(osmValue)) return e;
        }

        return null;
    }

    public static RouteType getEnumByGtfsValue(int gtfsValue){
        for(RouteType e : RouteType.values()){
            if(e.gtfsValue == gtfsValue) return e;
        }

        return null;
    }


    public int getGtfsValue() {
        return gtfsValue;
    }

    public String getOsmValue() {
        return osmValue;
    }
}
