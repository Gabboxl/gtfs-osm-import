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
package it.osm.gtfs.output;

import it.osm.gtfs.enums.WheelchairAccess;
import it.osm.gtfs.models.*;
import it.osm.gtfs.plugins.GTFSPlugin;
import it.osm.gtfs.utils.GTFSImportSettings;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class OSMRelationImportGenerator {

    //TODO: instead of using a StringBuilder to create the XML file, see the OSMBusImportGenerator class instead
    public static String createSingleTripRelation(BoundingBox bb, List<Integer> osmWaysIds, Trip trip, Route route, GTFSFeedInfo gtfsFeedInfo, int id) {
        GTFSPlugin plugin = GTFSImportSettings.getInstance().getPlugin();


        //todo: remove the timestamp as it is redundant, set osmosis' enableDateParsing option to false
        //the timestamp and the version attribute for every relation is needed by the merge with osmosis, unfortunately
        String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date());

        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?><osm version='0.6' generator='GTFSOSMImport'>\n");
        buffer.append(bb.getXMLTag());
        buffer.append("<relation id='-" + id + "' version='1' timestamp='" + currentTimeStamp +"' action='modify'>\n");


        for (OSMStop osmStop : trip.getStopsList().getStopSequenceOSMStopMap().values()) {
            buffer.append("<member type='node' ref='" + osmStop.originalXMLNode.getAttributes().getNamedItem("id").getNodeValue() + "' role='stop' />\n");
        }


        if (osmWaysIds != null) {
            for (Integer osmWayId : osmWaysIds) {
                buffer.append("<member type='way' ref=\"" + osmWayId + "\" role='' />\n");
            }
        }

        buffer.append("<tag k='type' v='route' />\n");
        buffer.append("<tag k='route' v='" + route.getRouteType().getOsmValue() + "' />\n");

        buffer.append("<tag k='public_transport:version' v='2' />\n");

        if (GTFSImportSettings.getInstance().useRevisedKey()) {
            buffer.append("<tag k='" + GTFSImportSettings.REVISED_KEY + "' v='no' />\n");
        }


        buffer.append("<tag k='name' v=\"" + StringUtils.capitalize(route.getRouteType().getOsmValue()) + " " + route.getShortName() + ": " + plugin.fixTripHeadsignName(trip.getTripHeadsign()) + "\" />\n");

        buffer.append("<tag k='ref' v=\"" + route.getShortName() + "\" />\n");

        buffer.append("<tag k='from' v=\"" + trip.getStopsList().getStopSequenceOSMStopMap().firstEntry().getValue().getName() + "\" />\n");
        buffer.append("<tag k='to' v=\"" + trip.getStopsList().getStopSequenceOSMStopMap().lastEntry().getValue().getName() + "\" />\n");

        buffer.append("<tag k='colour' v='#" + route.getRouteColor() + "' />\n");

        buffer.append("<tag k='network' v=\"" + GTFSImportSettings.getInstance().getNetwork() + "\" />\n");
        buffer.append("<tag k='operator' v=\"" + GTFSImportSettings.getInstance().getOperator() + "\" />\n");


        buffer.append("<tag k='gtfs:route_id' v='" + route.getId() + "' />\n");
        buffer.append("<tag k='gtfs:shape_id' v='" + trip.getShapeId() + "' />\n");
        buffer.append("<tag k='gtfs:agency_id' v='" + route.getAgencyId() + "' />\n");
        buffer.append("<tag k='gtfs:release_date' v='" + plugin.fixGtfsVersionDate(gtfsFeedInfo.getVersion()) + "' />\n");

        if (trip.getWheelchairAccess() != null && trip.getWheelchairAccess() != WheelchairAccess.UNKNOWN) {
            buffer.append("<tag k='wheelchair' v='" + trip.getWheelchairAccess().getOsmValue() + "' />\n");
        }

        buffer.append("</relation>");
        buffer.append("</osm>");

        return buffer.toString();
    }

    //TODO: to implement
    public static String createMasterRouteTripsRelation(Route route, List<Integer> idList, BoundingBox bb, int routeMasterId) {

        String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new java.util.Date());

        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?><osm version='0.6' generator='GTFSOSMImport'>\n");
        buffer.append(bb.getXMLTag());
        buffer.append("<relation id='-" + routeMasterId + "' version='1' timestamp='" + currentTimeStamp +"' action='modify'>\n");

        for (Integer childRelId : idList) {
            buffer.append("<member type='relation' ref=\"-" + childRelId + "\" role='' />\n");
        }

        buffer.append("<tag k='type' v='route_master' />\n");
        buffer.append("<tag k='route_master' v='" + route.getRouteType().getOsmValue() + "' />\n");
        buffer.append("<tag k='ref' v=\"" + route.getShortName() + "\" />\n");
        buffer.append("<tag k='name' v=\"" + StringUtils.capitalize(route.getRouteType().getOsmValue()) + " " + route.getShortName() + "\" />\n");
        buffer.append("<tag k='operator' v=\"" + GTFSImportSettings.getInstance().getOperator() + "\" />\n");
        buffer.append("<tag k='network' v=\"" + GTFSImportSettings.getInstance().getNetwork() + "\" />\n");
        buffer.append("<tag k='colour' v='#" + route.getRouteColor() + "' />\n");


        buffer.append("<tag k='gtfs:route_id' v='" + route.getId() + "' />\n");

        buffer.append("<tag k='gtfs:agency_id' v='" + route.getAgencyId() + "' />\n");


        buffer.append("</relation>");
        buffer.append("</osm>");

        return buffer.toString();
    }
}
