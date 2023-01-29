/**
 Licensed under the GNU General Public License version 3
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.gnu.org/licenses/gpl-3.0.html

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 **/
package it.osm.gtfs.input;

import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.models.Relation;
import it.osm.gtfs.models.Relation.OSMNode;
import it.osm.gtfs.models.Relation.OSMWay;
import it.osm.gtfs.models.Relation.RelationType;
import it.osm.gtfs.utils.GTFSImportSettings;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;

public class OSMParser {

    public static List<OSMStop> readOSMStops(String fileName, boolean readStopsOfAnyOperator) throws ParserConfigurationException, SAXException, IOException {
        List<OSMStop> osmStopsListOutput = new ArrayList<>();


        File file = new File(fileName);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("node");

        for (int s = 0; s < nodeList.getLength(); s++) {
            Node fstNode = nodeList.item(s);
            OSMStop osmStop = new OSMStop(null, null, new GeoPosition(Double.parseDouble(fstNode.getAttributes().getNamedItem("lat").getNodeValue()), Double.parseDouble(fstNode.getAttributes().getNamedItem("lon").getNodeValue())), null, null, null);
            osmStop.originalXMLNode = fstNode;

            //temp variables for tags
            String highwaytag = "",
                    railwaytag = "",
                    public_transporttag = "",
                    traintag = "",
                    tramtag = "",
                    bustag = "";

            NodeList att = fstNode.getChildNodes();

            for (int t = 0; t < att.getLength(); t++) {
                Node attNode = att.item(t);

                if (attNode.getAttributes() != null){
                    String key = attNode.getAttributes().getNamedItem("k").getNodeValue();
                    String value = attNode.getAttributes().getNamedItem("v").getNodeValue();

                    if (key.equalsIgnoreCase("ref"))
                        osmStop.setCode(value);

                    if (key.equalsIgnoreCase("name"))
                        osmStop.setName(value);

                    if (key.equalsIgnoreCase("operator"))
                        osmStop.setOperator(value);

                    if (key.equalsIgnoreCase("gtfs_id"))
                        osmStop.setGtfsId(value);

                    if (key.equalsIgnoreCase("highway"))
                        highwaytag = value;

                    if (key.equalsIgnoreCase("railway"))
                        railwaytag = value;

                    if (key.equalsIgnoreCase("public_transport") )
                        public_transporttag = value;

                    if (key.equalsIgnoreCase("train") )
                        traintag = value;

                    if (key.equalsIgnoreCase("tram"))
                        tramtag = value;

                    if (key.equalsIgnoreCase("bus"))
                        bustag = value;

                    if (key.equalsIgnoreCase("wheelchair"))
                        osmStop.setWheelchairAccessibility(WheelchairAccess.getEnumByOsmValue(value));

                    if (key.equalsIgnoreCase(GTFSImportSettings.getInstance().getRevisedKey()) && value.equalsIgnoreCase("yes"))
                        osmStop.setIsRevised(true);

                }
            }

            //osmstop type value setting

            if (railwaytag.equalsIgnoreCase("tram_stop"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAM_STOP);

            if (railwaytag.equalsIgnoreCase("station"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAM_STOP);

            if (traintag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAM_STOP);

            if (tramtag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_TRAM_STOP);

            if(highwaytag.equalsIgnoreCase("bus_stop"))
                osmStop.setStopType(OSMStopType.PHYSICAL_BUS_STOP);

            if (bustag.equalsIgnoreCase("yes"))
                osmStop.setStopType(OSMStopType.PHYSICAL_BUS_STOP);


            if (public_transporttag.equalsIgnoreCase("stop_position")) {
                OSMStopType currType = osmStop.getStopType();

                if(currType == null){
                    osmStop.setStopType(OSMStopType.GENERAL_STOP_POSITION);

                } else if (currType.equals(OSMStopType.PHYSICAL_BUS_STOP)) {
                    osmStop.setStopType(OSMStopType.BUS_STOP_POSITION);

                } else if(currType.equals(OSMStopType.PHYSICAL_TRAM_STOP)) {
                    osmStop.setStopType(OSMStopType.TRAM_STOP_POSITION);

                }
            }


            //if the current osm stop has a different operator tag value than the one specified in the properties we skip it
            if(!readStopsOfAnyOperator && (osmStop.getOperator() == null || !osmStop.getOperator().equalsIgnoreCase(GTFSImportSettings.getInstance().getOperator()))) {
                //System.out.println(osmStop.getOperator());

                System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Skipping OSM Stop node ID " + osmStop.getOSMId() + " (ref=" + osmStop.getCode() + ", gtfs_id=" + osmStop.getGtfsId() + ")" + " as its operator tag value (" + osmStop.getOperator() +") is different than the one specified in the properties file.").reset());
                continue;
            }


            if (osmStop.getStopType().equals(OSMStopType.GENERAL_STOP_POSITION))
                continue; //ignore unsupported stop positions (like ferries)

            if(osmStop.getStopType() == null) //we don't know the type of this stop based on the tag values we checked
                throw new IllegalArgumentException("Unknown node type for OSM node ID: " + osmStop.getOSMId() + ". We support only highway=bus_stop, public_transport=stop_position, railway=tram_stop and railway=station");


            osmStopsListOutput.add(osmStop);
        }

        return osmStopsListOutput;
    }

    public static List<Relation> readOSMRelations(File file, Map<String, OSMStop> stopsWithOSMIndex) throws SAXException, IOException{
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
            relationParser = new RelationParser(stopsWithOSMIndex, wayParser.result);
            xr.setContentHandler(relationParser);
            xr.setErrorHandler(relationParser);
            xr.parse(new InputSource(new FileReader(file)));
        }


        if (relationParser.missingNodes.size() > 0 || relationParser.failedRelationIds.size() > 0){
            System.out.println(ansi().render("@|red Failed to parse some relations. Relations IDs: |@" + StringUtils.join(relationParser.failedRelationIds, ", ")));
            System.out.println(ansi().render("@|red Failed to parse some relations. Missing nodes: |@" + StringUtils.join(relationParser.missingNodes, ", ")));
        }

        return relationParser.result;
    }

    private static class NodeParser extends DefaultHandler{
        private final Map<Long, OSMNode> result = new HashMap<>();

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {
            if (localName.equals("node")){
                result.put(Long.parseLong(attributes.getValue("id")),
                        new OSMNode(Double.parseDouble(attributes.getValue("lat")),
                                Double.parseDouble(attributes.getValue("lon"))));
            }
        }
    }

    private static class WayParser extends DefaultHandler{
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

            if (localName.equals("way")){
                currentWay = new OSMWay(Long.parseLong(attributes.getValue("id")));

            }else if(currentWay != null && localName.equals("nd")){
                currentWay.nodes.add(nodes.get(Long.parseLong(attributes.getValue("ref"))));

            }else if(currentWay != null && localName.equals("tag")){

                String key = attributes.getValue("k");
                String value = attributes.getValue("v");

                //we don't need to parse any way tag values/attributes at the moment
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName.equals("way")){
                result.put(currentWay.getId(), currentWay);
                currentWay = null;
            }
        }
    }

    private static class RelationParser extends DefaultHandler {
        private final Map<String, OSMStop> stopsWithOSMIndex;
        private final Map<Long, OSMWay> ways;

        private final List<Relation> result = new ArrayList<>();
        private final List<String> failedRelationIds = new ArrayList<>();
        private final List<String> missingNodes = new ArrayList<>();

        private Relation currentRelation;
        private long seq = 1;
        private boolean failed = false;

        private RelationParser(Map<String, OSMStop> stopsWithOSMIndex, Map<Long, OSMWay> ways) {
            super();
            this.stopsWithOSMIndex = stopsWithOSMIndex;
            this.ways = ways;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {

            if (localName.equals("relation")){
                currentRelation = new Relation(attributes.getValue("id"));
                currentRelation.setVersion(Integer.parseInt(attributes.getValue("version")));
                seq = 1;
                failed = false;
            }else if(currentRelation != null && localName.equals("member")){
                String type = attributes.getValue("type");
                String role = attributes.getValue("role");
                String ref = attributes.getValue("ref");

                if (type.equals("node")){
                    if (role.equals("stop") || role.equals("platform")){
                        OSMStop stop = stopsWithOSMIndex.get(ref);
                        if (stop == null){
                            System.out.println(ansi().render("@|red Warning: Node " +  ref + " not found in internal stops array/map. Probably this stop got marked as disused/abandoned or it's NOT a stop but is still attached to the relation " + currentRelation.getId() +"? |@"));
                            missingNodes.add(ref);
                            failed = true;
                        }
                        currentRelation.pushPoint(seq++, stop);
                    }else{
                        System.out.println(ansi().render("@|red Warning: Relation " + currentRelation.getId() + " has a member node with unsupported role: \"" + role +"\", node ref/ID=" + ref + "|@"));
                    }
                }else if (type.equals("way")){
                    OSMWay member = ways.get(Long.parseLong(attributes.getValue("ref")));
                    currentRelation.getWayMembers().add(member);
                }else{
                    System.out.println(ansi().render("@|red Warning: Relation " + currentRelation.getId() + " has an unsupported member of unknown type: \"" + type +"\"" + "|@"));
                }
            }else if (currentRelation != null && localName.equals("tag")){
                String key = attributes.getValue("k");
                if (key.equalsIgnoreCase("name"))
                    currentRelation.setName(attributes.getValue("v"));
                else if (key.equalsIgnoreCase("ref"))
                    currentRelation.setRef(attributes.getValue("v"));
                else if (key.equalsIgnoreCase("from"))
                    currentRelation.setFrom(attributes.getValue("v"));
                else if (key.equalsIgnoreCase("to"))
                    currentRelation.setTo(attributes.getValue("v"));
                else if (key.equalsIgnoreCase("route"))
                    try{
                        currentRelation.setType(RelationType.parse(attributes.getValue("v")));
                    }catch (IllegalArgumentException e){
                        e.printStackTrace();
                        failed = true;
                    }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName.equals("relation")){
                if (!failed){
                    result.add(currentRelation);
                }else{
                    failedRelationIds.add(currentRelation.getId());
                    System.out.println(ansi().render("@|red Warning: Failed to parse relation " + currentRelation.getId() + " [" + currentRelation.getName() + "]" + "|@"));
                }
                currentRelation = null;
            }
        }

    }
}
