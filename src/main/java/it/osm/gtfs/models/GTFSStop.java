package it.osm.gtfs.models;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.output.IElementCreator;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.OSMXMLUtils;
import org.jxmapviewer.viewer.GeoPosition;
import org.w3c.dom.Element;

public class GTFSStop extends Stop {

    public OSMStop osmStopMatchedWith;

    public OSMStop railwayStopMatchedWith; //this variable is useful to handle cases where the city OSM mappers use to map the bus stop near the highway, and then the tram stop position on the railway for the same stop (like in Turin)

    public GTFSStop(String gtfsId, String code, GeoPosition geoPosition, String name, String operator, OSMStopType stopType, WheelchairAccess wheelchairAccessibility) {
        super(gtfsId, code, geoPosition, name, operator, stopType, wheelchairAccessibility);
    }

    @Override
    public String toString() {
        return "Stop [gtfsId=" + getGtfsId() + ", code=" + getCode() + ", lat=" + getGeoPosition().getLatitude()
                + ", lon=" + getGeoPosition().getLongitude() + ", name=" + getName() + ", stopType=" + getStopType() + ", accessibility=" + getWheelchairAccessibility() + "]";
    }


    public Element getNewXMLNode(IElementCreator document){
        Element node = document.createElement("node");
        long id;
        try {
            id = Long.parseLong(getGtfsId());
        } catch(Exception e) {
            System.err.println("The gtfs_id=" + getGtfsId() + " isn't numerical, using an hashcode for the new OSM node instead...");
            id = Math.abs(getGtfsId().hashCode());
        }

        node.setAttribute("id", "-" + id);
        node.setAttribute("visible", "true");
        node.setAttribute("lat", String.valueOf(getGeoPosition().getLatitude()));
        node.setAttribute("lon", String.valueOf(getGeoPosition().getLongitude()));

        node.appendChild(OSMXMLUtils.createTagElement(document, "name", GTFSImportSettings.getInstance().getPlugin().fixBusStopName(this)));
        node.appendChild(OSMXMLUtils.createTagElement(document, "ref", getCode()));
        node.appendChild(OSMXMLUtils.createTagElement(document, "gtfs_id", getGtfsId()));
        node.appendChild(OSMXMLUtils.createTagElement(document, "operator", GTFSImportSettings.getInstance().getOperator()));
        node.appendChild(OSMXMLUtils.createTagElement(document, "wheelchair", getWheelchairAccessibility().getOsmValue()));

        if(GTFSImportSettings.getInstance().useRevisedKey()) {
            node.appendChild(OSMXMLUtils.createTagElement(document, GTFSImportSettings.REVISED_KEY, "no"));
        }

        //different node values based on the stop type

        if(getStopType().equals(OSMStopType.PHYSICAL_BUS_STOP)) {
            node.appendChild(OSMXMLUtils.createTagElement(document, "bus", "yes"));
            node.appendChild(OSMXMLUtils.createTagElement(document, "highway", "bus_stop"));
            node.appendChild(OSMXMLUtils.createTagElement(document, "public_transport", "platform"));

        } else if (getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP)) { //todo: check the appropriate tags
            node.appendChild(OSMXMLUtils.createTagElement(document, "railway", "station"));
            node.appendChild(OSMXMLUtils.createTagElement(document, "station", "subway"));
            node.appendChild(OSMXMLUtils.createTagElement(document, "subway", "yes"));
            //node.appendChild(OSMXMLUtils.createTagElement(document, "train", "yes"));

        } else if (getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION)) {
            node.appendChild(OSMXMLUtils.createTagElement(document, "public_transport", "station"));
            node.appendChild(OSMXMLUtils.createTagElement(document, "railway", "station"));
            node.appendChild(OSMXMLUtils.createTagElement(document, "train", "yes"));
        }


        return node;
    }


}
