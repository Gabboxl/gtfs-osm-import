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
package it.osm.gtfs.utils;

import it.osm.gtfs.output.IElementCreator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
        if (tag == null){
            addTagAndValue(node, key, value);
        } else {
            addOSMModifyActionAttribute(node);

            tag.setAttribute("v", value);
        }
    }

    public static void addTagIfNotExisting(Element node, String key, String value) {
        if (getTagElement(node, key) == null) {
            addTagAndValue(node, key, value);

            addOSMModifyActionAttribute(node);
        }
    }

    private static Element getTagElement(Element node, String key){
        NodeList childs = node.getChildNodes();
        for (int t = 0; t < childs.getLength(); t++) {
            Node attNode = childs.item(t);
            if (attNode.getAttributes() != null){
                if (attNode.getAttributes().getNamedItem("k").getNodeValue().equalsIgnoreCase(key)){ //check if the key equals without case check
                    return (Element) attNode;
                }
            }
        }
        return null;
    }


    public static String getTagValue(Element node, String key){
        Element attNode = getTagElement(node, key);

        if (attNode != null){
            return attNode.getNodeValue();
        }

        return null;
    }

    public static Element createTagElement(IElementCreator document, String key, String value){
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

            if(GTFSImportSettings.getInstance().useRevisedKey()) {
                removeOldRevisedTag(node); //we remove old Turin-specific revised tags
                addOrReplaceTagValue(node, GTFSImportSettings.REVISED_KEY, "no");
            }
        }

        if (tagElementHighway != null) {
            //node.removeChild(tagElementHighway);
            tagElementHighway.setAttribute("k", "disused:railway");
        }

        if (tagElementRailway != null) {
            //node.removeChild(tagElementRailway);
            tagElementRailway.setAttribute("k", "disused:highway");
        }

        if (tagElementPublicTransport != null) {
            //node.removeChild(tagElementPublicTransport);
            tagElementPublicTransport.setAttribute("k", "disused:public_transport");
        }
    }

    public static void removeOldRevisedTag(Element node) {
        var oldtag1 = getTagElement(node, "GTT:Revised");
        var oldtag2 = getTagElement(node, "GTT:revised");

        if (oldtag1 != null)
            node.removeChild(oldtag1);

        if (oldtag2 != null)
            node.removeChild(oldtag2);
    }
}
