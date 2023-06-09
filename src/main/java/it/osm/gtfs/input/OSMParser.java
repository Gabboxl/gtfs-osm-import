/**
 * Licensed under the GNU General Public License version 3
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package it.osm.gtfs.input;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.enums.RouteType;
import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.models.ReadOSMRelationsResult;
import it.osm.gtfs.models.Relation;
import it.osm.gtfs.models.Relation.OSMNode;
import it.osm.gtfs.models.Relation.OSMWay;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.SharedCliOptions;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.jxmapviewer.viewer.GeoPosition;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;

public class OSMParser {

    public static List<OSMStop> readOSMStops(String osmStopsFileName, boolean readStopsOfAnyOperator) throws ParserConfigurationException, SAXException, IOException {
        List<OSMStop> osmStopsListOutput = new ArrayList<>();


        File osmStopsFile = new File(osmStopsFileName);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(osmStopsFile);
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("node");

        for (int s = 0; s < nodeList.getLength(); s++) {
            Node fstNode = nodeList.item(s);
            OSMStop osmStop = new OSMStop(null, null, new GeoPosition(Double.parseDouble(fstNode.getAttributes().getNamedItem("lat").getNodeValue()), Double.parseDouble(fstNode.getAttributes().getNamedItem("lon").getNodeValue())), null, null, null, null);
            osmStop.originalXMLNode = fstNode;

            //temp variables for tags
            String highway_tag = "",
                    railway_tag = "",
                    public_transport_tag = "",
                    train_tag = "",
                    tram_tag = "",
                    bus_tag = "",
                    station_tag = "",
                    subway_tag = "";

            NodeList att = fstNode.getChildNodes();

            for (int t = 0; t < att.getLength(); t++) {
                Node attNode = att.item(t);

                if (attNode.getAttributes() != null) {
                    String key = attNode.getAttributes().getNamedItem("k").getNodeValue();
                    String value = attNode.getAttributes().getNamedItem("v").getNodeValue();

                    if (StringUtils.containsIgnoreCase(key, "disused")) {
                        osmStop.setDisused(true);
                        key = key.replace("disused:", ""); //we remove the disused part from the key name so that we can continue setting the stop's data
                    }


                    if (key.equalsIgnoreCase("ref"))
                        osmStop.setCode(value);

                    if (key.equalsIgnoreCase("name"))
                        osmStop.setName(value);

                    if (key.equalsIgnoreCase("operator"))
                        osmStop.setOperator(value);

                    if (key.equalsIgnoreCase("gtfs_id"))
                        osmStop.setGtfsId(value);

                    if (key.equalsIgnoreCase("highway"))
                        highway_tag = value;

                    if (key.equalsIgnoreCase("railway"))
                        railway_tag = value;

                    if (key.equalsIgnoreCase("public_transport"))
                        public_transport_tag = value;

                    if (key.equalsIgnoreCase("train"))
                        train_tag = value;

                    if (key.equalsIgnoreCase("tram"))
                        tram_tag = value;

                    if (key.equalsIgnoreCase("bus"))
                        bus_tag = value;

                    if (key.equalsIgnoreCase("station"))
                        station_tag = value;

                    if (key.equalsIgnoreCase("subway"))
                        subway_tag = value;

                    if (key.equalsIgnoreCase("wheelchair"))
                        osmStop.setWheelchairAccessibility(WheelchairAccess.getEnumByOsmValue(value));

                    if (key.equalsIgnoreCase(GTFSImportSettings.REVISED_KEY) && value.equalsIgnoreCase("yes"))
                        osmStop.setIsRevised(true);

                }
            }

            //osmstop type value setting

            if (railway_tag.equalsIgnoreCase("station"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAIN_STATION);

            if (public_transport_tag.equalsIgnoreCase("station"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAIN_STATION);

            if (train_tag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAIN_STATION);

            if (tram_tag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAM_STOP);

            //bus

            if (highway_tag.equalsIgnoreCase("bus_stop"))
                osmStop.setStopType(OSMStopType.PHYSICAL_BUS_STOP);

            if (bus_tag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_BUS_STOP);

            //tram

            if (railway_tag.equalsIgnoreCase("tram_stop"))
                osmStop.setStopType(OSMStopType.TRAM_STOP_POSITION);

            //subway
            if (station_tag.equalsIgnoreCase("subway"))
                osmStop.setStopType(OSMStopType.PHYSICAL_SUBWAY_STOP);

            if (subway_tag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_SUBWAY_STOP);

            if (public_transport_tag.equalsIgnoreCase("stop_position")) {
                OSMStopType currType = osmStop.getStopType();

                if (currType == null) {
                    osmStop.setStopType(OSMStopType.GENERAL_STOP_POSITION);

                } else if (currType.equals(OSMStopType.PHYSICAL_BUS_STOP)) {
                    osmStop.setStopType(OSMStopType.BUS_STOP_POSITION);

                } else if (currType.equals(OSMStopType.PHYSICAL_TRAM_STOP)) {
                    osmStop.setStopType(OSMStopType.TRAM_STOP_POSITION);

                } else if (currType.equals(OSMStopType.PHYSICAL_TRAIN_STATION)) {
                    osmStop.setStopType(OSMStopType.TRAIN_STOP_POSITION);

                } else if (currType.equals(OSMStopType.PHYSICAL_SUBWAY_STOP)) {
                    osmStop.setStopType(OSMStopType.SUBWAY_STOP_POSITION);

                }
            }


            //skip subway stops if requested
            if (SharedCliOptions.onlyBusStops && (osmStop.getStopType().equals(OSMStopType.PHYSICAL_SUBWAY_STOP) || osmStop.getStopType().equals(OSMStopType.PHYSICAL_TRAIN_STATION))) {

                System.out.println(ansi().render("@|yellow Skipping OSM subway/station stop (nodeID= " + osmStop.getOSMId() + ", ref= " + osmStop.getCode() + ", gtfs_id=" + osmStop.getGtfsId() + ") as requested. |@"));
                continue;
            }


            //if the current osm stop has a different operator tag value than the one specified in the properties we skip it - but we keep the stops with a null operator as they could be of our operator
            if (!readStopsOfAnyOperator && osmStop.getOperator() != null && !StringUtils.containsIgnoreCase(osmStop.getOperator(), GTFSImportSettings.getInstance().getOperator())) {
                //System.out.println(osmStop.getOperator());

                System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Skipping OSM Stop node ID " + osmStop.getOSMId() + " (ref=" + osmStop.getCode() + ", gtfs_id=" + osmStop.getGtfsId() + ")" + " as its operator tag value (" + osmStop.getOperator() + ") is different than the one specified in the properties file.").reset());
                continue;
            }


            if (osmStop.getStopType() == null) //we don't know the type of this stop based on the tag values we checked
                throw new IllegalArgumentException("Unknown node type for OSM node ID: " + osmStop.getOSMId() + ". We support only highway=bus_stop, public_transport=stop_position, railway=tram_stop, railway=station and station=subway");


            if (osmStop.getStopType().equals(OSMStopType.GENERAL_STOP_POSITION)) {
                System.out.println(ansi().render("@|yellow Ignoring general_stop_position... (node ID: " + osmStop.getOSMId() + ") |@"));
                continue; //ignore unsupported stop positions (like ferries)
            }


            osmStopsListOutput.add(osmStop);
        }

        return osmStopsListOutput;
    }

    public static ReadOSMRelationsResult readOSMRelations(File file, Map<String, OSMStop> stopsWithOSMIndex, boolean readRelationsOfAnyOperator) throws SAXException, IOException {
        NodeParser nodeParser;
        {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            nodeParser = new NodeParser();
            xr.setContentHandler(nodeParser);
            xr.setErrorHandler(nodeParser);
            xr.parse(new InputSource(new FileReader(file)));
        }

        WayParser wayParser;
        {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            wayParser = new WayParser(nodeParser.result);
            xr.setContentHandler(wayParser);
            xr.setErrorHandler(wayParser);
            xr.parse(new InputSource(new FileReader(file)));
        }

        RelationParser relationParser;
        {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            relationParser = new RelationParser(stopsWithOSMIndex, wayParser.result, readRelationsOfAnyOperator);
            xr.setContentHandler(relationParser);
            xr.setErrorHandler(relationParser);
            xr.parse(new InputSource(new FileReader(file)));
        }


        if (relationParser.missingNodes.size() > 0 || relationParser.failedRelations.size() > 0) {
            List<String> failedRelsIds = new ArrayList<>();

            for (Relation failedRel : relationParser.failedRelations) {
                failedRelsIds.add(failedRel.getId());
            }

            System.out.println(ansi().render("@|red OSMParser: " + relationParser.failedRelations.size() + " relations could't be parsed because of not valid member nodes. Relations IDs: |@" + StringUtils.join(failedRelsIds, ", ")));
            System.out.println(ansi().render("@|red OSMParser: " + relationParser.missingNodes.size() + " member nodes are not valid stops. Invalid nodes: |@" + StringUtils.join(relationParser.missingNodes, ", ")));
        }

        return new ReadOSMRelationsResult(relationParser.validRelations, relationParser.failedRelations, relationParser.missingNodes);
    }

    private static class NodeParser extends DefaultHandler {
        private final Map<Long, OSMNode> result = new HashMap<>();

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {
            if (localName.equals("node")) {

                OSMNode osmNode = new OSMNode(new GeoPosition(Double.parseDouble(attributes.getValue("lat")),
                        Double.parseDouble(attributes.getValue("lon"))), Long.parseLong(attributes.getValue("id")), null);

                result.put(Long.parseLong(attributes.getValue("id")), osmNode);

            }
        }
    }

    private static class WayParser extends DefaultHandler {
        Map<Long, OSMNode> nodes;

        Map<Long, OSMWay> result = new HashMap<>();
        OSMWay currentWay;

        private WayParser(Map<Long, OSMNode> nodes) {
            super();
            this.nodes = nodes;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {

            if (localName.equals("way")) {
                currentWay = new OSMWay(Long.parseLong(attributes.getValue("id")));

            } else if (currentWay != null && localName.equals("nd")) { //aggiungiamo all'oggetto way tutti i nodi che la compongono
                currentWay.nodes.add(nodes.get(Long.parseLong(attributes.getValue("ref"))));

            } else if (currentWay != null && localName.equals("tag")) {

                String key = attributes.getValue("k");
                String value = attributes.getValue("v");

                //we don't need to parse any way tag values/attributes at the moment
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName.equals("way")) {
                result.put(currentWay.getId(), currentWay);
                currentWay = null;
            }
        }
    }

    private static class RelationParser extends DefaultHandler {
        private final Map<String, OSMStop> stopsWithOSMIndex;
        private final Map<Long, OSMWay> ways;

        private final List<Relation> validRelations = new ArrayList<>();
        private final List<Relation> failedRelations = new ArrayList<>();
        private final List<String> missingNodes = new ArrayList<>();

        private final boolean readRelationsOfAnyOperator;

        //temp tags variables
        String route_tag, type_tag;
        Map<String, String> tempMemberRefRoleMap;


        private Relation currentRelation;
        private long seq = 1;
        private boolean failed = false;

        private RelationParser(Map<String, OSMStop> stopsWithOSMIndex, Map<Long, OSMWay> ways, boolean readRelationsOfAnyOperator) {
            super();
            this.stopsWithOSMIndex = stopsWithOSMIndex;
            this.ways = ways;
            this.readRelationsOfAnyOperator = readRelationsOfAnyOperator;

        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {

            if (localName.equals("relation")) {
                currentRelation = new Relation(attributes.getValue("id"));
                currentRelation.setVersion(Integer.parseInt(attributes.getValue("version")));
                seq = 1;
                failed = false;

                route_tag = "";
                type_tag = "";

                tempMemberRefRoleMap = new HashMap<>();

            } else if (currentRelation != null && localName.equals("member")) {
                String memberType = attributes.getValue("type");
                String memberRole = attributes.getValue("role");
                String memberRef = attributes.getValue("ref");

                if (memberType.equals("node")) {
                    tempMemberRefRoleMap.put(memberRef, memberRole);

                } else if (memberType.equals("way")) {
                    OSMWay member = ways.get(Long.parseLong(attributes.getValue("ref")));
                    currentRelation.getWayMembers().add(member);

                } else { //TODO: supportare i membri "relation", ovvero le master_relation solitamente
                    System.out.println(ansi().render("@|red Warning: Relation " + currentRelation.getId() + " has a member (id: " + memberRef + ") of an unsupported type \"" + memberType + "\"" + "|@"));
                }

            } else if (currentRelation != null && localName.equals("tag")) {
                String key = attributes.getValue("k");

                if (key.equalsIgnoreCase("type"))
                    type_tag = attributes.getValue("v");

                if (key.equalsIgnoreCase("name"))
                    currentRelation.setName(attributes.getValue("v"));

                else if (key.equalsIgnoreCase("ref"))
                    currentRelation.setRef(attributes.getValue("v"));

                else if (key.equalsIgnoreCase("from"))
                    currentRelation.setFrom(attributes.getValue("v"));

                else if (key.equalsIgnoreCase("to"))
                    currentRelation.setTo(attributes.getValue("v"));

                else if (key.equalsIgnoreCase("operator")) {
                    currentRelation.setOperator(attributes.getValue("v"));

                } else if (key.equalsIgnoreCase("route")) {
                    route_tag = attributes.getValue("v");

                }
            }
        }

        //here we check the relation data we gathered during the parsing
        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName.equals("relation")) {

                if (!type_tag.equalsIgnoreCase("route")) {
                    System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Skipping OSM relation " + currentRelation.getId() + " as its type tag (" + type_tag + ") is not a route.").reset());

                    return;
                }


                //if the current osm relation has a different operator tag value than the one specified in the properties we skip it - but we keep the stops with a null operator as they could be of our operator
                if (!readRelationsOfAnyOperator && currentRelation.getOperator() != null && !StringUtils.containsIgnoreCase(currentRelation.getOperator(), GTFSImportSettings.getInstance().getOperator())) {

                    System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Skipping OSM relation " + currentRelation.getId() + " as its operator tag value (" + currentRelation.getOperator() + ") is different than the one specified in the properties file.").reset());

                    return;
                }


                //members check
                for (var entry : tempMemberRefRoleMap.entrySet()) {
                    var tempMemberRole = entry.getValue();
                    var tempMemberRef = entry.getKey();

                    //array with the supported roles
                    String[] supportedRoles = new String[]{"stop", "platform", "stop_exit_only", "stop_entry_only", "platform_exit_only", "platform_entry_only"};

                    if (Arrays.asList(supportedRoles).contains(tempMemberRole) ) {
                        OSMStop osmStop = stopsWithOSMIndex.get(tempMemberRef);

                        if (osmStop == null) {
                            System.out.println(ansi().render("@|yellow Warning: Node " + tempMemberRef + " not found in internal stops array/map. Probably this isn't a valid stop anymore but is still attached to the relation " + currentRelation.getId() + ". Better checking it out. |@"));
                            missingNodes.add(tempMemberRef);
                            failed = true;
                        }
                        currentRelation.pushPoint(seq++, osmStop);

                    } else {
                        System.out.println(ansi().render("@|red Warning: Relation " + currentRelation.getId() + " has a member node with an unsupported role \"" + tempMemberRole + "\", node ref/Id = " + tempMemberRef + "|@"));
                    }
                }

                //route tag
                try {
                    currentRelation.setRouteType(RouteType.getEnumByOsmValue(route_tag));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    failed = true;
                }


                if (!failed) {
                    validRelations.add(currentRelation);
                } else {
                    failedRelations.add(currentRelation);
                    System.out.println(ansi().render("@|red OSMParser: Relation " + currentRelation.getId() + " couldn't be parsed because of invalid member nodes. [" + currentRelation.getName() + "]" + "|@"));
                }

            }
        }

    }
}
