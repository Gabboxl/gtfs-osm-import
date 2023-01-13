package it.osm.gtfs.models;

import java.util.Map;
import java.util.Set;

public class ReadStopTimesResult {
    private final Map<String, TripStopsList> tripIdStopListMap;
    private final Set<String> missingStops;

    public ReadStopTimesResult(Map<String, TripStopsList> tripIdStopListMap, Set<String> missingStops) {
        this.tripIdStopListMap = tripIdStopListMap;
        this.missingStops = missingStops;
    }

    public Map<String, TripStopsList> getTripIdStopListMap() {
        return tripIdStopListMap;
    }

    public Set<String> getMissingStops() {
        return missingStops;
    }
}