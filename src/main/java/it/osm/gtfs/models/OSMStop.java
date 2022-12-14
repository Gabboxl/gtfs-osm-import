package it.osm.gtfs.models;

import it.osm.gtfs.enums.WheelchairAccess;
import org.jxmapviewer.viewer.GeoPosition;
import org.w3c.dom.Node;

public class OSMStop extends Stop {

    private boolean needsPositionReview = false;
    private boolean isRevised = false;

    private Boolean isBusOrTramStopPosition = false; //TODO: maybe rename to just isStopPosition?

    public GTFSStop gtfsStopMatchedWith;
    public Node originalXMLNode;


    public OSMStop(String gtfsId, String code, GeoPosition geoPosition, String name, String operator, WheelchairAccess wheelchairAccessibility) {
        super(gtfsId, code, geoPosition, name, operator, wheelchairAccessibility);
    }

    public String getOSMId() {
        return (originalXMLNode == null) ? null : originalXMLNode.getAttributes().getNamedItem("id").getNodeValue();
    }

    public void setNeedsPositionReview(boolean needsGuiReview) {
        this.needsPositionReview = needsGuiReview;
    }

    public void setIsRevised(boolean isRevised) {
        this.isRevised = isRevised;
    }

    public void setIsBusOrTramStopPosition(Boolean isBusStopPosition){
        this.isBusOrTramStopPosition = isBusStopPosition;
    }

    public Boolean isBusOrTramStopPosition(){
        return isBusOrTramStopPosition;
    }

    public boolean isRevised() {
        return isRevised;
    }

    public boolean needsPositionReview() {
        return needsPositionReview;
    }


    @Override
    public String toString() {
        return "Stop [gtfsId=" + getGtfsId() + ", code=" + getCode() + ", lat=" + getGeoPosition().getLatitude()
                + ", lon=" + getGeoPosition().getLongitude() + ", name=" + getName() + ", operator=" + getOperator() + ", accessibility=" + getWheelchairAccessibility() +
                ((originalXMLNode != null) ? ", osmid=" + getOSMId() : "" )
                + ", isTramStop=" + isTramStop() + ", isBusStopPosition=" + isBusOrTramStopPosition() +
                ", isRevised=" + isRevised() + "]";
    }
}
