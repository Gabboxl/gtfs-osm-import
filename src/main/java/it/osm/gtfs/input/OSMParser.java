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

import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.model.OSMStop;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.OSMDistanceUtils;
import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Relation.OSMNode;
import it.osm.gtfs.model.Relation.OSMRelationWayMember;
import it.osm.gtfs.model.Relation.OSMWay;
import it.osm.gtfs.model.Relation.RelationType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.fusesource.jansi.Ansi;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import static org.fusesource.jansi.Ansi.ansi;

public class OSMParser {

    public static List<OSMStop> readOSMStops(String fileName, boolean readStopsOfAnyOperator) throws ParserConfigurationException, SAXException, IOException {
        List<OSMStop> osmStopsListOutput = new ArrayList<OSMStop>();
        Multimap<String, OSMStop> refBuses = HashMultimap.create(); //?
        Multimap<String, OSMStop> refRails = HashMultimap.create(); //??

        File file = new File(fileName);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("node");

        for (int s = 0; s < nodeList.getLength(); s++) {
            Node fstNode = nodeList.item(s);
            OSMStop osmStop = new OSMStop(null, null, Double.valueOf(fstNode.getAttributes().getNamedItem("lat").getNodeValue()), Double.valueOf(fstNode.getAttributes().getNamedItem("lon").getNodeValue()), null, null, null);
            osmStop.originalXMLNode = fstNode;
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
                    if (key.equalsIgnoreCase("highway") && value.equalsIgnoreCase("bus_stop"))
                        osmStop.setIsTramStop(false);
                    if (key.equalsIgnoreCase("railway") && value.equalsIgnoreCase("tram_stop"))
                        osmStop.setIsTramStop(true);
                    if (key.equalsIgnoreCase("railway") && value.equalsIgnoreCase("station"))
                        osmStop.setIsTramStop(true);
                    if (key.equalsIgnoreCase("public_transport") && value.equalsIgnoreCase("stop_position") && osmStop.isTramStop() == null)
                        osmStop.setIsBusOrTramStopPosition(true);
                    if (key.equalsIgnoreCase("train") && value.equalsIgnoreCase("yes"))
                        osmStop.setIsTramStop(true);
                    if (key.equalsIgnoreCase("tram") && value.equalsIgnoreCase("yes"))
                        osmStop.setIsTramStop(true);
                    if (key.equalsIgnoreCase("bus") && value.equalsIgnoreCase("yes"))
                        osmStop.setIsTramStop(false);
                    if (key.equalsIgnoreCase("wheelchair") && value.equalsIgnoreCase("no"))
                        osmStop.setWheelchairAccessibility(WheelchairAccess.NO);
                    if (key.equalsIgnoreCase("wheelchair") && value.equalsIgnoreCase("limited"))
                        osmStop.setWheelchairAccessibility(WheelchairAccess.LIMITED);
                    if (key.equalsIgnoreCase(GTFSImportSettings.getInstance().getRevisedKey()) && value.equalsIgnoreCase("yes"))
                        osmStop.setIsRevised(true);

                }
            }

            //if the current osm stop has a different operator tag value than the one specified in the properties we skip it
            if(!readStopsOfAnyOperator && (osmStop.getOperator() == null || !osmStop.getOperator().equalsIgnoreCase(GTFSImportSettings.getInstance().getOperator()))) {
                //System.out.println(osmStop.getOperator());

                System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Skipping OSM Stop node ID " + osmStop.getOSMId() + " (ref=" + osmStop.getCode() + ", gtfs_id=" + osmStop.getGtfsId() + ")" + " as its operator tag value (" + osmStop.getOperator() +") is different than the one specified in the properties file.").reset());
                continue;
            }

            if (osmStop.isTramStop() == null)
                if (osmStop.isBusOrTramStopPosition())
                    continue; //ignore unsupported stop positions (like ferries)
                else
                    throw new IllegalArgumentException("Unknown node type for OSM node ID: " + osmStop.getOSMId() + ". We support only highway=bus_stop, public_transport=stop_position, railway=tram_stop and railway=station");

            //Check duplicate ref in osm
            if (osmStop.getCode() != null){
                if (osmStop.isBusOrTramStopPosition() == null || !osmStop.isBusOrTramStopPosition()){
                    if (osmStop.isTramStop()){
                        if (refRails.containsKey(osmStop.getCode())){
                            for (OSMStop existingStop : refRails.get(osmStop.getCode())){
                                if (OSMDistanceUtils.distVincenty(osmStop.getLat(), osmStop.getLon(), existingStop.getLat(), existingStop.getLon()) < 500)
                                    System.err.println("Warning: The ref " + osmStop.getCode() + " is used in more than one node within 500m this may lead to bad import." +
                                            " (node IDs :" + osmStop.getOSMId() + "," + existingStop.getOSMId() + ")");
                            }
                        }

                        refRails.put(osmStop.getCode(), osmStop);
                    }else{
                        if (refBuses.containsKey(osmStop.getCode())){
                            for (OSMStop existingStop : refBuses.get(osmStop.getCode())){
                                if (OSMDistanceUtils.distVincenty(osmStop.getLat(), osmStop.getLon(), existingStop.getLat(), existingStop.getLon()) < 500)
                                    System.err.println("Warning: The ref " + osmStop.getCode() + " is used in more than one node within 500m this may lead to bad import." +
                                            " (node IDs :" + osmStop.getOSMId() + "," + existingStop.getOSMId() + ")");
                            }
                        }
                        refBuses.put(osmStop.getCode(), osmStop);
                    }
                }
            }
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
            System.err.println("Failed to parse some relations. Relations IDs: " + StringUtils.join(relationParser.failedRelationIds, ", "));
            System.err.println("Failed to parse some relations. Missing nodes: " + StringUtils.join(relationParser.missingNodes, ", "));
        }

        return relationParser.result;
    }

    private static class NodeParser extends DefaultHandler{
        private final Map<Long, OSMNode> result = new HashMap<Long, OSMNode>();

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

        Map<Long, OSMWay> result = new HashMap<Long, Relation.OSMWay>();
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

                if (key.equalsIgnoreCase("oneway")){
                    if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true")){
                        currentWay.oneway = true;
                    }else if (value.equalsIgnoreCase("no") || value.equalsIgnoreCase("false")){
                        currentWay.oneway = false;
                    }else{
                        System.err.println("Unhandled oneway attribute: " + value + " way id: " + currentWay.getId());
                    }
                }else if (key.equalsIgnoreCase("junction")){
                    if (value.equalsIgnoreCase("roundabout")){
                        currentWay.oneway = true;
                    }
                }
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

    private static class RelationParser extends DefaultHandler{
        private final Map<String, OSMStop> stopsWithOSMIndex;
        private final Map<Long, OSMWay> ways;

        private final List<Relation> result = new ArrayList<Relation>();
        private final List<String> failedRelationIds = new ArrayList<String>();
        private final List<String> missingNodes = new ArrayList<String>();

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
                            System.err.println("Warning: Node " +  ref + " not found in internal stops array/map. Probably this stop got marked as disused/abandoned or it's NOT a stop but is still attached to the relation " + currentRelation.getId() +"?");
                            missingNodes.add(ref);
                            failed = true;
                        }
                        currentRelation.pushPoint(seq++, stop);
                    }else{
                        System.err.println("Warning: Relation " + currentRelation.getId() + " has a member node with unsupported role: \"" + role +"\", node ref/ID=" + ref);
                    }
                }else if (type.equals("way")){
                        OSMRelationWayMember member = new OSMRelationWayMember();
                        member.way = ways.get(Long.parseLong(attributes.getValue("ref")));
                        currentRelation.getWayMembers().add(member);
                }else{
                    System.err.println("Warning: Relation " + currentRelation.getId() + " has an unsupported member of unknown type: \"" + type +"\"");
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
                        System.err.println(e.getMessage());
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
                    System.err.println("Warning: Failed to parse relation " + currentRelation.getId() + " [" + currentRelation.getName() + "]");
                }
                currentRelation = null;
            }
        }

    }
}
