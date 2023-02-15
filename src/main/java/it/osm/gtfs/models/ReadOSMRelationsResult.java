package it.osm.gtfs.models;

import java.util.List;

public class ReadOSMRelationsResult {
    private final List<Relation> finalValidRelations;
    private final List<Relation> failedRelations;
    private final List<String> missingNodes;

    public ReadOSMRelationsResult(List<Relation> finalValidRelations, List<Relation> failedRelations, List<String> missingNodes) {
        this.finalValidRelations = finalValidRelations;
        this.failedRelations = failedRelations;
        this.missingNodes = missingNodes;
    }


    public List<Relation> getFinalValidRelations() {
        return finalValidRelations;
    }

    public List<Relation> getFailedRelations() {
        return failedRelations;
    }

    public List<String> getMissingNodes() {
        return missingNodes;
    }
}