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
import it.osm.gtfs.plugins.GTFSPlugin;
import it.osm.gtfs.utils.GTFSImportSettings;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class OSMRelationImportGenerator {

    public static String createSingleTripRelation(BoundingBox bb, List<Integer> osmWaysIds, Trip trip, Route route, GTFSFeedInfo gtfsFeedInfo){
        GTFSPlugin plugin = GTFSImportSettings.getInstance().getPlugin();

        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?><osm version='0.6' generator='JOSM'>");
        buffer.append(bb.getXMLTag());
        buffer.append("<relation id='-" + Math.round(Math.random() * 100000) +  "'>\n");


        for (OSMStop s : trip.getStopsList().getStopSequenceOSMStopMap().values()){
            buffer.append("<member type='node' ref='" + s.originalXMLNode.getAttributes().getNamedItem("id").getNodeValue() + "' role='stop' />\n");
        }


        if(osmWaysIds != null) {
            for (Integer osmWayId : osmWaysIds) {
                buffer.append("<member type='way' ref='" + osmWayId + "' role='' />\n");
            }
        }

        //todo: trip ip
        buffer.append("<tag k='type' v='route' />\n");
        buffer.append("<tag k='route' v='" + route.getRouteType().getOsmValue() + "' />\n");

        buffer.append("<tag k='public_transport:version' v='2' />\n");

        //todo: da mettere a posto i nomi, from e to
        buffer.append("<tag k='name' v='" + StringUtils.capitalize(route.getRouteType().getOsmValue()) + " " + route.getShortName() + ": " + plugin.fixTripHeadsignName(trip.getTripHeadsign()) + "' />\n");

        buffer.append("<tag k='ref' v='" + route.getShortName() + "' />\n");

        buffer.append("<tag k='from' v='"+ trip.getStopsList().getStopSequenceOSMStopMap().firstEntry().getValue().getName() + "' />\n");
        buffer.append("<tag k='to' v='" + trip.getStopsList().getStopSequenceOSMStopMap().lastEntry().getValue().getName() + "' />\n");

        buffer.append("<tag k='colour' v='#" + route.getRouteColor() + "' />\n");

        buffer.append("<tag k='network' v='" + GTFSImportSettings.getInstance().getNetwork() + "' />\n");
        buffer.append("<tag k='operator' v='" + GTFSImportSettings.getInstance().getOperator() + "' />\n");


        //todo: aggiungere gtfs shape id?
        buffer.append("<tag k='gtfs:route_id' v='" + route.getId() + "' />\n");
        buffer.append("<tag k='gtfs:shape_id' v='" + trip.getShapeId() + "' />\n");
        buffer.append("<tag k='gtfs:trip_id' v='" + trip.getTripId() + "' />\n"); //maybe al posto del trip id che identifica un singolo giro in una giornata metterre service_id?
        buffer.append("<tag k='gtfs:agency_id' v='" + route.getAgencyId() + "' />\n");
        buffer.append("<tag k='gtfs:release_date' v='" + plugin.fixGtfsVersionDate(gtfsFeedInfo.getVersion()) + "' />\n");

        buffer.append("<tag k='wheelchair' v='" + trip.getWheelchairAccess().getOsmValue() + "' />\n");

        buffer.append("</relation>");
        buffer.append("</osm>");

        return buffer.toString();
    }

    public static String createMasterRouteTripsRelation() {
        return "side";
    }
}
