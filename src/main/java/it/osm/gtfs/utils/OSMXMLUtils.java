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

public class OSMXMLUtils {

	public static void addTag(Element node, String key, String value) {
		node.setAttribute("action", "modify");
		Element e = node.getOwnerDocument().createElement("tag");
		e.setAttribute("k", key);
		e.setAttribute("v", value);
		node.appendChild(e);
	}

	public static void addTagOrReplace(Element node, String key, String value) {
		Element tag = getTagElement(node, key);
		if (tag == null){
			addTag(node, key, value);
		}else{
			node.setAttribute("action", "modify");
			tag.setAttribute("v", value);
		}
	}
	
	public static void addTagIfNotExisting(Element node, String key, String value) {
		if (getTagElement(node, key) == null)
			addTag(node, key, value);
	}
	
	private static Element getTagElement(Element node, String key){
		NodeList childs = node.getChildNodes();
		for (int t = 0; t < childs.getLength(); t++) {
			Node attNode = childs.item(t);
			if (attNode.getAttributes() != null){
				if (attNode.getAttributes().getNamedItem("k").getNodeValue().equals(key)){
					return (Element) attNode;
				}
			}
		}
		return null;
	}

	public static Element createTagElement(IElementCreator document, String key, String value){
		Element tag = document.createElement("tag");
		tag.setAttribute("k", key);
		tag.setAttribute("v", value);
		return tag;
	}
}
