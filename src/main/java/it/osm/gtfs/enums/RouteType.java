package it.osm.gtfs.enums;

public enum RouteType { //data according to the GTFS reference guide

    LIGHT_RAIL(0, "light_rail"), //only for osm
    TRAM(0, "tram"),
    SUBWAY_METRO(1, "subway"),
    RAIL(2, "train"),
    BUS(3, "bus"),
    FERRY(4, "ferry"),
    CABLE_TRAM(5, "tram"), //only for gtfs
    AERIAL_LIFT(6, "funicular"), //only for gtfs
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
//        for(RouteType e : RouteType.values()){
//            if(e.osmValue.equals(osmValue)) return e;
//        }

        //unfortunately we have to hardcode the various key-value pairs because GTFS' and OSM's route types are different

        if (osmValue != null){
            if (osmValue.equalsIgnoreCase("bus"))
                return BUS;
            if (osmValue.equalsIgnoreCase("tram"))
                return TRAM;
            if (osmValue.equalsIgnoreCase("subway"))
                return SUBWAY_METRO;
            if (osmValue.equalsIgnoreCase("train"))
                return RAIL;
            if (osmValue.equalsIgnoreCase("light_rail"))
                return LIGHT_RAIL;
            if (osmValue.equalsIgnoreCase("funicular"))
                return FUNICULAR;
            if (osmValue.equalsIgnoreCase("ferry"))
                return FERRY;
            if (osmValue.equalsIgnoreCase("trolleybus"))
                return TROLLEYBUS;
            if (osmValue.equalsIgnoreCase("monorail"))
                return MONORAIL;
        }

        throw new IllegalArgumentException("Unsupported relation type: \"" + osmValue + "\"");
    }

    public static RouteType getEnumByGtfsValue(int gtfsValue){
//        for(RouteType e : RouteType.values()){
//            if(e.gtfsValue == gtfsValue) return e;
//        }

        if (gtfsValue == 0)
            return TRAM;
        if (gtfsValue == 1)
            return SUBWAY_METRO;
        if (gtfsValue == 2)
            return RAIL;
        if (gtfsValue == 3)
            return BUS;
        if (gtfsValue == 4)
            return FERRY;
        if (gtfsValue == 5)
            return CABLE_TRAM;
        if (gtfsValue == 6)
            return AERIAL_LIFT;
        if (gtfsValue == 7)
            return FUNICULAR;
        if (gtfsValue == 11)
            return TROLLEYBUS;
        if (gtfsValue == 12)
            return MONORAIL;

        throw new IllegalArgumentException("Unsupported GTFS type: \"" + gtfsValue + "\"");
    }


    public int getGtfsValue() {
        return gtfsValue;
    }

    public String getOsmValue() {
        return osmValue;
    }
}
