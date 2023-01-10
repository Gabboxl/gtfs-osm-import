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
package it.osm.gtfs.output;

import it.osm.gtfs.models.*;
import it.osm.gtfs.utils.GTFSImportSettings;

import java.util.List;

public class OSMRelationImportGenerator {

    //FIXME: refactor
    public static String getRelation(BoundingBox bb, StopsList stopTimes, List<Integer> osmWaysIds, Trip t, Route r){
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?><osm version='0.6' generator='JOSM'>");
        buffer.append(bb.getXMLTag());
        buffer.append("<relation id='-" + Math.round(Math.random()*100000) +  "'>\n");

        for (OSMStop s : stopTimes.getSeqOSMStopMap().values()){
            //OSMStop s1 = (OSMStop) s;
            buffer.append("<member type='node' ref='" + s.originalXMLNode.getAttributes().getNamedItem("id").getNodeValue() + "' role='stop' />\n");
        }


        if(osmWaysIds != null) {
            for (Integer osmWayId : osmWaysIds) {
                buffer.append("<member type='way' ref='" + osmWayId + "' role='' />\n");
            }
        }

        buffer.append("<tag k='direction' v='" + GTFSImportSettings.getInstance().getPlugin().fixTripName(t.getName()) + "' />\n");
        buffer.append("<tag k='name' v='" + r.getShortName() + ": " + r.getLongName().replaceAll("'", "\'") + "' />\n");
        buffer.append("<tag k='network' v='" + GTFSImportSettings.getInstance().getNetwork() + "' />\n");
        buffer.append("<tag k='operator' v='" + GTFSImportSettings.getInstance().getOperator() + "' />\n");
        buffer.append("<tag k='ref' v='" + r.getShortName() + "' />\n");
        buffer.append("<tag k='route' v='bus' />\n");
        buffer.append("<tag k='type' v='route' />\n");
        buffer.append("</relation>");
        buffer.append("</osm>");

        return buffer.toString();
    }
}
