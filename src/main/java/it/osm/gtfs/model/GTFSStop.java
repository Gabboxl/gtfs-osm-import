package it.osm.gtfs.model;

import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.output.IElementCreator;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.OSMXMLUtils;
import org.w3c.dom.Element;

public class GTFSStop extends Stop {


    public GTFSStop(String gtfsId, String code, Double lat, Double lon, String name, String operator, WheelchairAccess wheelchairAccessibility) {
        super(gtfsId, code, lat, lon, name, operator, wheelchairAccessibility);
    }

    @Override
    public String toString() {
        return "Stop [gtfsId=" + getGtfsId() + ", code=" + getCode() + ", lat=" + getLat()
                + ", lon=" + getLon() + ", name=" + getName() + ", accessibility=" + getWheelchairAccessibility() + "]";
    }

    public Element getNewXMLNode(IElementCreator document){ //TODO: I think we need to support different combinations of tags for tram stops/metro stops/bus stops
        Element node = document.createElement("node");
        long id;
        try{
            id = Long.parseLong(getGtfsId());
        }catch(Exception e){
            id = Math.abs(getGtfsId().hashCode());
        }

        node.setAttribute("id", "-" + id);
        node.setAttribute("visible", "true");
        node.setAttribute("lat", getLat().toString());
        node.setAttribute("lon", getLon().toString());
        node.appendChild(OSMXMLUtils.createTagElement(document, "bus", "yes"));
        node.appendChild(OSMXMLUtils.createTagElement(document, "highway", "bus_stop"));
        node.appendChild(OSMXMLUtils.createTagElement(document, "public_transport", "platform"));
        node.appendChild(OSMXMLUtils.createTagElement(document, "operator", GTFSImportSettings.getInstance().getOperator()));
        node.appendChild(OSMXMLUtils.createTagElement(document, GTFSImportSettings.getInstance().getRevisedKey(), "no"));
        node.appendChild(OSMXMLUtils.createTagElement(document, "name", GTFSImportSettings.getInstance().getPlugin().fixBusStopName(getName())));
        node.appendChild(OSMXMLUtils.createTagElement(document, "ref", getCode()));
        node.appendChild(OSMXMLUtils.createTagElement(document, "gtfs_id", getGtfsId()));
        node.appendChild(OSMXMLUtils.createTagElement(document, "wheelchair", getWheelchairAccessibility().getOsmValue()));
        return node;
    }


}
