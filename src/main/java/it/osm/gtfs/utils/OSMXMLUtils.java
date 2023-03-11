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
package it.osm.gtfs.utils;

import it.osm.gtfs.output.IElementCreator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/***
 * This class contains methods to work with XML OSM data
 */
public class OSMXMLUtils {

    public static void addTagAndValue(Element node, String key, String value) {
        addOSMModifyActionAttribute(node);

        Element e = node.getOwnerDocument().createElement("tag");
        e.setAttribute("k", key);
        e.setAttribute("v", value);
        node.appendChild(e);
    }

    public static void addOrReplaceTagValue(Element node, String key, String value) {
        Element tag = getTagElement(node, key);

        if (tag == null) {
            addTagAndValue(node, key, value);
        } else {

            tag.setAttribute("v", value);
        }

        addOSMModifyActionAttribute(node);
    }

    public static void addTagIfNotExisting(Element node, String key, String value) {
        if (getTagElement(node, key) == null) {
            addTagAndValue(node, key, value);

            addOSMModifyActionAttribute(node);
        }
    }

    private static Element getTagElement(Element node, String key) {
        NodeList childs = node.getChildNodes();
        for (int t = 0; t < childs.getLength(); t++) {
            Node attNode = childs.item(t);
            if (attNode.getAttributes() != null) {
                if (attNode.getAttributes().getNamedItem("k").getNodeValue().equalsIgnoreCase(key)) { //check if the key equals without case check
                    return (Element) attNode;
                }
            }
        }
        return null;
    }


    public static String getTagValue(Element node, String key) {
        Element attNode = getTagElement(node, key);

        if (attNode != null) {
            return attNode.getNodeValue();
        }

        return null;
    }

    public static Element createTagElement(IElementCreator document, String key, String value) {
        Element tag = document.createElement("tag");
        tag.setAttribute("k", key);
        tag.setAttribute("v", value);
        return tag;
    }

    public static void addOSMModifyActionAttribute(Element node) {
        node.setAttribute("action", "modify");
    }

    public static void addOSMDeleteActionAttribute(Element node) {
        //node.setAttribute("action", "delete");
    }

    //TODO: probably we should move this method to the StopUtils class
    public static void markDisused(Element node) {
        var tagElementHighway = getTagElement(node, "highway");
        var tagElementRailway = getTagElement(node, "railway");
        var tagElementPublicTransport = getTagElement(node, "public_transport");

        if (tagElementHighway != null || tagElementRailway != null || tagElementPublicTransport != null) {
            addOSMModifyActionAttribute(node);

            if (GTFSImportSettings.getInstance().useRevisedKey()) {
                removeOldRevisedTag(node); //we remove old Turin-specific revised tags
                addOrReplaceTagValue(node, GTFSImportSettings.REVISED_KEY, "no");
            }
        }

        if (tagElementHighway != null) {
            //node.removeChild(tagElementHighway);
            tagElementHighway.setAttribute("k", "disused:highway");
        }

        if (tagElementRailway != null) {
            //node.removeChild(tagElementRailway);
            tagElementRailway.setAttribute("k", "disused:railway");
        }

        if (tagElementPublicTransport != null) {
            //node.removeChild(tagElementPublicTransport);
            tagElementPublicTransport.setAttribute("k", "disused:public_transport");
        }


        LocalDateTime current = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = current.format(formatter);

        String disusedNote = "This stop got marked as disused on " + formattedDate + " as it was removed from the transport agency's GTFS data." +
                " It can be a temporary removal or the stop could have been physically removed. Please check and update this node.";

        //we add a note to the stop node telling the people when and why this stop was marked as disused
        addOrReplaceTagValue(node, "note:disused", disusedNote);
    }

    public static void unmarkDisused(Element node) {
        NodeList childNodes = node.getChildNodes();

        for (int t = 0; t < childNodes.getLength(); t++) {
            Node attNode = childNodes.item(t);
            if (attNode.getAttributes() != null) {

                String attNodeKeyValue = attNode.getAttributes().getNamedItem("k").getNodeValue();

                if (attNodeKeyValue.startsWith("disused:")) {

                    attNode.getAttributes().getNamedItem("k").setNodeValue(attNodeKeyValue.replace("disused:", ""));

                }
            }
        }

        addOSMModifyActionAttribute(node);
    }

    public static void removeOldRevisedTag(Element node) {
        var oldtag1 = getTagElement(node, "GTT:Revised");

        if (oldtag1 != null)
            node.removeChild(oldtag1);

    }
}
