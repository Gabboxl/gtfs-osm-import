package it.osm.gtfs.models;

import it.osm.gtfs.enums.RouteType;
import org.jxmapviewer.viewer.GeoPosition;

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
    private String operator;
    private RouteType routeType;
    private List<OSMWay> wayMembers = new ArrayList<>();

    private Map<Long, OSMStop> sequenceOSMstopMap;

    public Relation(String id) {
        this.id = id;
        sequenceOSMstopMap = new TreeMap<>();
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

    public int getStopsAffinity(TripStopsList tripStopsList) {
        boolean exactMatch = true;
        int affinity = 0;

        for (OSMStop stop : sequenceOSMstopMap.values())
            if (tripStopsList.getStopSequenceOSMStopMap().containsValue(stop)) {
                affinity += sequenceOSMstopMap.size() - Math.abs((getKeysByValue(sequenceOSMstopMap, stop) - getKeysByValue(tripStopsList.getStopSequenceOSMStopMap(), stop)));
            } else {
                affinity -= sequenceOSMstopMap.size();
                exactMatch = false;
            }

        int diff = Math.abs(tripStopsList.getStopSequenceOSMStopMap().size() - sequenceOSMstopMap.size());

        if (exactMatch && diff == 0)
            return Integer.MAX_VALUE;

        affinity -= diff;
        return affinity;
    }

    public Map<Long, OSMStop> getStops() {
        return sequenceOSMstopMap;
    }

    public void setStops(Map<Long, OSMStop> s) {
        sequenceOSMstopMap = s;
    }


    public boolean equalsStops(TripStopsList o) {
        if (sequenceOSMstopMap.size() != o.getStopSequenceOSMStopMap().size())
            return false;
        for (Long key : o.getStopSequenceOSMStopMap().keySet()) {
            Stop a = sequenceOSMstopMap.get(key);
            Stop b = o.getStopSequenceOSMStopMap().get(key);
            if (a == null || !a.equals(b))
                return false;
        }
        return true;
    }


    public void pushPoint(Long sequence, OSMStop stop) {
        sequenceOSMstopMap.put(sequence, stop);
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

    public RouteType getRouteType() {
        return routeType;
    }

    public void setRouteType(RouteType routeType) {
        this.routeType = routeType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<OSMWay> getWayMembers() {
        return wayMembers;
    }

    public void setWayMembers(List<OSMWay> wayMembers) {
        this.wayMembers = wayMembers;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public static class OSMWay {
        private final long id;
        public List<OSMNode> nodes = new ArrayList<>();

        public OSMWay(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class OSMNode {
        private final GeoPosition geoPosition;
        private final Long id;
        private final String role;

        public OSMNode(GeoPosition geoPosition, Long id, String role) {
            super();
            this.geoPosition = geoPosition;
            this.id = id;
            this.role = role;
        }

        public GeoPosition getGeoPosition() {
            return geoPosition;
        }

        public Long getId() {
            return id;
        }

        public String getRole() {
            return role;
        }
    }
}