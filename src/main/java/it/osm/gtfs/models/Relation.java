package it.osm.gtfs.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Relation {
    private final String id;
    private String name;
    private Integer version;
    private String ref;
    private String from;
    private String to;
    private RelationType type;
    private List<OSMRelationWayMember> wayMembers = new ArrayList<>();

    private Map<Long, OSMStop> osmstops;

    public Relation(String id) {
        this.id = id;
        osmstops = new TreeMap<>();
    }

    public int getStopsAffinity(StopsList o) {
        boolean exactMatch = true;
        int affinity = 0;
        for (OSMStop s:osmstops.values())
            if (o.getSeqOSMStopMap().containsValue(s)){
                affinity+= osmstops.size() - Math.abs((getKeysByValue(osmstops, s) - getKeysByValue(o.getSeqOSMStopMap(), s)));
            }else{
                affinity -= osmstops.size();
                exactMatch = false;
            }
        int diff = Math.abs(o.getSeqOSMStopMap().size() - osmstops.size());

        if (exactMatch && diff == 0)
            return Integer.MAX_VALUE;

        affinity -= diff;
        return affinity;
    }


    //TODO: maybe this function can be put in a separate Utils class
    private static <T, E> T getKeysByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<Long, OSMStop> getStops() {
        return osmstops;
    }

    public void setStops(Map<Long, OSMStop> s){
        osmstops = s;
    }


    public boolean equalsStops(StopsList o) {
        if (osmstops.size() != o.getSeqOSMStopMap().size())
            return false;
        for (Long key: o.getSeqOSMStopMap().keySet()){
            Stop a = osmstops.get(key);
            Stop b = o.getSeqOSMStopMap().get(key);
            if (a == null || !a.equals(b))
                return false;
        }
        return true;
    }




    public void pushPoint(Long seq, OSMStop stop){
        osmstops.put(seq, stop);
    }

    public String getId() {
        return id;
    }


    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<OSMRelationWayMember> getWayMembers() {
        return wayMembers;
    }

    public void setWayMembers(List<OSMRelationWayMember> wayMembers) {
        this.wayMembers = wayMembers;
    }

    public enum RelationType{
        SUBWAY(0), TRAM(1), BUS(2), TRAIN(3), LIGHT_RAIL(4);

        private final int dbId;
        RelationType(int dbId){
            this.dbId = dbId;
        }

        public static RelationType parse(String nodeValue) {
            if (nodeValue != null){
                if (nodeValue.equalsIgnoreCase("bus"))
                    return BUS;
                if (nodeValue.equalsIgnoreCase("tram"))
                    return TRAM;
                if (nodeValue.equalsIgnoreCase("subway"))
                    return SUBWAY;
                if (nodeValue.equalsIgnoreCase("train"))
                    return TRAIN;
                if (nodeValue.equalsIgnoreCase("light_rail"))
                    return LIGHT_RAIL;
            }
            throw new IllegalArgumentException("unsupported relation type: " + nodeValue);
        }

        public int dbId() {
            return dbId;
        }

    }

    public static class OSMRelationWayMember{
        public OSMWay way;
    }

    public static class OSMWay {
        private final long id;
        public List<OSMNode> nodes = new ArrayList<>();
        public boolean oneway = false;

        public OSMWay(long id){
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class OSMNode {
        private final Double lat;
        private final Double lon;

        public OSMNode(Double lat, Double lon) {
            super();
            this.lat = lat;
            this.lon = lon;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLon() {
            return lon;
        }
    }
}